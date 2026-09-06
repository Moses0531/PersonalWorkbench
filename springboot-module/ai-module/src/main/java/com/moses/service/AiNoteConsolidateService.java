package com.moses.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.entity.Note;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记 AI 整合：多条碎片 → 一篇整理稿（digest）。
 */
@Slf4j
@Service
public class AiNoteConsolidateService {

    private static final int MAX_TEXT_PER_NOTE = 8000;
    private static final int MAX_TOTAL_TEXT = 28000;
    private static final int MIN_SINGLE_CONTENT = 50;
    private static final int MAX_INSTRUCTION = 500;

    private static final String SYSTEM_PROMPT = """
            你是 RemoAssistant 的笔记整理助手。根据用户勾选的零散碎片，整理成一篇清晰可读的整理稿。
            要求：
            - 使用中文 Markdown
            - 结构建议：主题概述 / 要点归纳 / 待办与线索 / 原文摘录（若材料中无则省略对应节）
            - 忠实于材料，不要编造未出现的事实
            - 合并重复信息，理清逻辑关系
            - 不要输出 JSON，不要用 markdown 代码块包裹全文
            """;

    private final ChatClient planningChatClient;
    private final NoteService noteService;
    private final ObjectMapper objectMapper;

    public AiNoteConsolidateService(@Qualifier("planningChatClient") ChatClient planningChatClient,
                                    NoteService noteService,
                                    ObjectMapper objectMapper) {
        this.planningChatClient = planningChatClient;
        this.noteService = noteService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> consolidate(Long userId, Map<String, Object> body) {
        List<Long> noteIds = parseNoteIds(body != null ? body.get("noteIds") : null);
        if (noteIds.isEmpty()) {
            throw new RuntimeException("请选择至少一条碎片笔记");
        }

        List<Note> notes = noteService.listOwnedByIds(userId, noteIds);
        Map<Long, Note> byId = new LinkedHashMap<>();
        for (Note n : notes) {
            byId.put(n.getNoteId(), n);
        }
        List<Note> ordered = new ArrayList<>();
        for (Long id : noteIds) {
            Note n = byId.get(id);
            if (n == null) {
                throw new RuntimeException("笔记不存在或无权访问: " + id);
            }
            if (!Note.TYPE_SCRAP.equals(n.getNoteType())) {
                throw new RuntimeException("只能整合碎片笔记，请取消勾选整理稿");
            }
            ordered.add(n);
        }

        int totalChars = ordered.stream()
                .mapToInt(n -> (n.getContent() != null ? n.getContent().length() : 0)
                        + (n.getTitle() != null ? n.getTitle().length() : 0))
                .sum();
        if (ordered.size() == 1 && totalChars < MIN_SINGLE_CONTENT) {
            throw new RuntimeException("单条碎片内容过短，请再选几条或补充正文后再整合");
        }

        String titleHint = text(body != null ? body.get("title") : null);
        String instruction = text(body != null ? body.get("instruction") : null);
        if (instruction.length() > MAX_INSTRUCTION) {
            throw new RuntimeException("补充说明不能超过 " + MAX_INSTRUCTION + " 字");
        }

        String content;
        try {
            content = planningChatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserPrompt(ordered, titleHint, instruction))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 笔记整合失败, userId={}, noteIds={}", userId, noteIds, e);
            throw new RuntimeException("AI 整合失败，请稍后重试");
        }

        String digest = stripFence(content);
        if (!StringUtils.hasText(digest)) {
            throw new RuntimeException("AI 未生成有效整理稿，请重试");
        }

        String digestTitle = StringUtils.hasText(titleHint)
                ? titleHint
                : extractTitle(digest, ordered);

        Note created = new Note();
        created.setTitle(digestTitle);
        created.setContent(digest);
        created.setNoteType(Note.TYPE_DIGEST);
        created.setStatus(Note.STATUS_ACTIVE);
        created.setSourceNoteIds(writeIdsJson(noteIds));
        created.setRemark("AI整合");

        Note saved = noteService.createOwnedNote(userId, created);

        Map<String, Object> result = new HashMap<>();
        result.put("note", saved);
        result.put("noteId", saved.getNoteId());
        result.put("title", saved.getTitle());
        result.put("content", saved.getContent());
        result.put("sourceNoteIds", noteIds);
        return result;
    }

    private String buildUserPrompt(List<Note> notes, String titleHint, String instruction) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(titleHint)) {
            sb.append("期望整理稿标题：").append(titleHint).append('\n');
        }
        if (StringUtils.hasText(instruction)) {
            sb.append("用户补充说明：").append(instruction).append('\n');
        }
        sb.append("共 ").append(notes.size()).append(" 条碎片，请整合：\n\n");

        int budget = MAX_TOTAL_TEXT;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            sb.append("--- 碎片 ").append(i + 1).append(" (id=").append(n.getNoteId()).append(") ---\n");
            if (StringUtils.hasText(n.getTitle())) {
                sb.append("标题：").append(n.getTitle().trim()).append('\n');
            }
            if (n.getUpdateTime() != null) {
                sb.append("更新时间：").append(fmt.format(n.getUpdateTime())).append('\n');
            }
            String body = n.getContent() != null ? n.getContent().trim() : "";
            if (body.length() > MAX_TEXT_PER_NOTE) {
                body = body.substring(0, MAX_TEXT_PER_NOTE) + "\n…(已截断)";
            }
            if (body.length() > budget) {
                body = body.substring(0, Math.max(0, budget)) + "\n…(已截断)";
                budget = 0;
            } else {
                budget -= body.length();
            }
            sb.append(body).append("\n\n");
            if (budget <= 0) {
                sb.append("（后续碎片因长度限制已省略）\n");
                break;
            }
        }
        return sb.toString();
    }

    private static String extractTitle(String digest, List<Note> sources) {
        for (String line : digest.split("\n")) {
            String t = line.trim();
            if (t.startsWith("#")) {
                t = t.replaceFirst("^#+\\s*", "").trim();
                if (StringUtils.hasText(t)) {
                    return t.length() > 80 ? t.substring(0, 80) : t;
                }
            }
        }
        if (!sources.isEmpty() && StringUtils.hasText(sources.get(0).getTitle())) {
            return sources.get(0).getTitle().trim() + " · 整理";
        }
        return "笔记整理稿 " + new SimpleDateFormat("MM-dd HH:mm").format(new Date());
    }

    private static String stripFence(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String summary = content.trim();
        if (summary.startsWith("```")) {
            int start = summary.indexOf('\n');
            int end = summary.lastIndexOf("```");
            if (start >= 0 && end > start) {
                summary = summary.substring(start + 1, end).trim();
            }
        }
        return summary;
    }

    private List<Long> parseNoteIds(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return ids;
        }
        for (Object item : list) {
            Long id = toLong(item);
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private String writeIdsJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new RuntimeException("来源笔记序列化失败");
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(value).trim();
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
