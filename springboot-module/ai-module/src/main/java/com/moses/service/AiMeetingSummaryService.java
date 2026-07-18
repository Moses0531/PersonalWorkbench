package com.moses.service;

import com.moses.entity.WbMeeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 会议材料 AI 整理：读取附件元数据与可读文本，生成概要并写回会议。
 */
@Slf4j
@Service
public class AiMeetingSummaryService {

    private static final int MAX_TEXT_PER_FILE = 12000;
    private static final int MAX_TOTAL_TEXT = 28000;
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(12);

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是个人工作台的会议纪要助手。根据会议基本信息与上传材料，整理成清晰可读的会议概要。
            要求：
            - 使用中文 Markdown
            - 结构建议：会议背景 / 讨论要点 / 结论与决策 / 后续事项（若材料中无则省略对应节）
            - 忠实于材料，不要编造未出现的事实
            - 不要输出 JSON，不要用 markdown 代码块包裹全文
            """;

    private final ChatClient planningChatClient;
    private final WbMeetingService wbMeetingService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(FETCH_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public AiMeetingSummaryService(@Qualifier("planningChatClient") ChatClient planningChatClient,
                                   WbMeetingService wbMeetingService) {
        this.planningChatClient = planningChatClient;
        this.wbMeetingService = wbMeetingService;
    }

    public Map<String, Object> summarize(Long userId, Long meetingId) {
        if (meetingId == null) {
            throw new RuntimeException("会议 ID 不能为空");
        }
        WbMeeting meeting = wbMeetingService.requireOwnedMeeting(userId, meetingId);
        List<Map<String, Object>> allAttachments = wbMeetingService.parseAttachments(meeting.getAttachments());
        List<Map<String, Object>> attachments = allAttachments.stream()
                .filter(a -> !"ai-summary".equals(String.valueOf(a.get("kind"))))
                .toList();
        if (attachments.isEmpty()) {
            throw new RuntimeException("请先上传会议材料，再进行 AI 整理");
        }

        String content;
        try {
            content = planningChatClient.prompt()
                    .system(SUMMARY_SYSTEM_PROMPT)
                    .user(buildUserPrompt(meeting, attachments))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 会议整理失败, userId={}, meetingId={}", userId, meetingId, e);
            throw new RuntimeException("AI 整理失败，请稍后重试");
        }

        String summary = content != null ? content.trim() : "";
        if (!StringUtils.hasText(summary)) {
            throw new RuntimeException("AI 未生成有效概要，请检查材料后重试");
        }
        if (summary.startsWith("```")) {
            int start = summary.indexOf('\n');
            int end = summary.lastIndexOf("```");
            if (start >= 0 && end > start) {
                summary = summary.substring(start + 1, end).trim();
            }
        }

        Map<String, Object> summaryFile = wbMeetingService.applyAiSummary(userId, meetingId, summary);

        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("aiSummary", summary);
        result.put("status", "1");
        if (summaryFile != null) {
            result.put("summaryFile", summaryFile);
        }
        return result;
    }

    private String buildUserPrompt(WbMeeting meeting, List<Map<String, Object>> attachments) {
        StringBuilder sb = new StringBuilder();
        sb.append("会议标题：").append(nullToEmpty(meeting.getTitle())).append('\n');
        if (meeting.getMeetingTime() != null) {
            sb.append("会议时间：")
                    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(meeting.getMeetingTime()))
                    .append('\n');
        }
        if (StringUtils.hasText(meeting.getLocation())) {
            sb.append("地点：").append(meeting.getLocation().trim()).append('\n');
        }
        if (StringUtils.hasText(meeting.getParticipants())) {
            sb.append("参会人：").append(meeting.getParticipants().trim()).append('\n');
        }

        sb.append("\n会议材料列表：\n");
        int textBudget = MAX_TOTAL_TEXT;
        for (int i = 0; i < attachments.size(); i++) {
            Map<String, Object> item = attachments.get(i);
            String name = String.valueOf(item.getOrDefault("name", "未命名"));
            String url = String.valueOf(item.getOrDefault("url", ""));
            String mime = String.valueOf(item.getOrDefault("mime", ""));
            sb.append(i + 1).append(". ").append(name);
            if (StringUtils.hasText(mime)) {
                sb.append("（").append(mime).append('）');
            }
            sb.append('\n');

            if (textBudget <= 0 || !isReadableText(name, mime)) {
                continue;
            }
            String extracted = fetchTextContent(url, Math.min(MAX_TEXT_PER_FILE, textBudget));
            if (!StringUtils.hasText(extracted)) {
                continue;
            }
            textBudget -= extracted.length();
            sb.append("--- 材料正文《").append(name).append("》开始 ---\n");
            sb.append(extracted).append('\n');
            sb.append("--- 材料正文《").append(name).append("》结束 ---\n");
        }

        sb.append("\n请根据以上信息整理会议概要。");
        return sb.toString();
    }

    private static boolean isReadableText(String name, String mime) {
        String lowerName = name != null ? name.toLowerCase(Locale.ROOT) : "";
        String lowerMime = mime != null ? mime.toLowerCase(Locale.ROOT) : "";
        if (lowerMime.startsWith("text/")
                || lowerMime.contains("json")
                || lowerMime.contains("xml")
                || lowerMime.contains("markdown")) {
            return true;
        }
        return lowerName.endsWith(".txt")
                || lowerName.endsWith(".md")
                || lowerName.endsWith(".markdown")
                || lowerName.endsWith(".json")
                || lowerName.endsWith(".csv")
                || lowerName.endsWith(".log")
                || lowerName.endsWith(".xml")
                || lowerName.endsWith(".html")
                || lowerName.endsWith(".htm");
    }

    private String fetchTextContent(String url, int maxChars) {
        if (!StringUtils.hasText(url) || maxChars <= 0) {
            return "";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(FETCH_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return "";
            }
            String text = new String(body, StandardCharsets.UTF_8);
            if (text.length() > maxChars) {
                return text.substring(0, maxChars) + "\n…（内容过长已截断）";
            }
            return text;
        } catch (Exception e) {
            log.warn("读取会议材料文本失败: {}", e.getMessage());
            return "";
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
