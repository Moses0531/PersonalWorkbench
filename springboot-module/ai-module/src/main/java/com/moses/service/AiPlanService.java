package com.moses.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.entity.WbProject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 项目 AI 规划：预览用结构化 JSON（Map），落板走 WbTaskService。
 */
@Slf4j
@Service
public class AiPlanService {

    private static final int MAX_GOAL_LENGTH = 2000;
    private static final int MAX_CONSTRAINTS_LENGTH = 1000;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String PLAN_SYSTEM_PROMPT = """
            你是个人工作台的项目规划助手。根据项目主题、目标与【整项目截止时间】拆解阶段与步骤。
            只输出一个 JSON 对象，不要 markdown 代码块或其它文字。字段：
            summary(string), phases(array)。
            phases 元素：title, description, dueTime(yyyy-MM-dd，该阶段建议完成日), steps。
            steps 元素：title, description, dueTime(yyyy-MM-dd), priority(int可选), status("0"待办/"1"进行中，默认"0")。
            工期策略（必须遵守）：
            - 剩余 ≤14 天：阶段 2～3 个，每阶段 2～3 步，只保留必须交付，压缩并行与验收。
            - 剩余 15～45 天：阶段 3～4 个，节奏均衡，含必要缓冲。
            - 剩余 >45 天：阶段 4～6 个，可含调研/打磨，步骤可更细，但仍要落在截止日前。
            所有 dueTime 不得晚于项目截止日；阶段 dueTime 应随顺序递增；summary 首句点明剩余天数与拆解节奏。
            """;

    private final ChatClient planningChatClient;
    private final WbProjectService wbProjectService;
    private final WbTaskService wbTaskService;
    private final ObjectMapper objectMapper;

    public AiPlanService(@Qualifier("planningChatClient") ChatClient planningChatClient,
                         WbProjectService wbProjectService,
                         WbTaskService wbTaskService,
                         ObjectMapper objectMapper) {
        this.planningChatClient = planningChatClient;
        this.wbProjectService = wbProjectService;
        this.wbTaskService = wbTaskService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> preview(Long userId, Map<String, Object> body) {
        Long projectId = toLong(body != null ? body.get("projectId") : null);
        if (projectId == null) {
            throw new RuntimeException("项目 ID 不能为空");
        }
        WbProject project = requireOwnedProject(userId, projectId);
        if ("1".equals(String.valueOf(project.getStatus()))) {
            throw new RuntimeException("项目已归档，无法规划");
        }

        LocalDate deadline = parseDeadline(body != null ? body.get("deadline") : null);
        if (deadline == null) {
            throw new RuntimeException("请填写项目截止时间");
        }
        LocalDate today = LocalDate.now();
        if (deadline.isBefore(today)) {
            throw new RuntimeException("项目截止时间不能早于今天");
        }
        long daysLeft = ChronoUnit.DAYS.between(today, deadline);
        String paceHint = paceHint(daysLeft);

        String goal = text(body.get("goal"));
        if (!StringUtils.hasText(goal)) {
            goal = project.getDescription() != null ? project.getDescription().trim() : "";
        }
        if (!StringUtils.hasText(goal)) {
            goal = "推进并完成项目「" + project.getName() + "」";
        }
        if (goal.length() > MAX_GOAL_LENGTH) {
            throw new RuntimeException("规划目标不能超过 " + MAX_GOAL_LENGTH + " 字");
        }
        String constraints = text(body.get("constraints"));
        if (constraints.length() > MAX_CONSTRAINTS_LENGTH) {
            throw new RuntimeException("约束说明不能超过 " + MAX_CONSTRAINTS_LENGTH + " 字");
        }

        String content;
        try {
            content = planningChatClient.prompt()
                    .system(PLAN_SYSTEM_PROMPT)
                    .user(buildUserPrompt(project, goal, constraints, deadline, daysLeft, paceHint))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI 规划预览失败, userId={}, projectId={}", userId, projectId, e);
            throw new RuntimeException("AI 规划失败，请稍后重试");
        }

        Map<String, Object> preview = parsePlanJson(content);
        List<Map<String, Object>> phases = normalizePhases(preview.get("phases"), deadline);
        if (phases.isEmpty()) {
            throw new RuntimeException("AI 未生成有效阶段，请补充目标后重试");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("planBatchId", UUID.randomUUID().toString().replace("-", ""));
        result.put("summary", preview.get("summary") != null ? String.valueOf(preview.get("summary")) : "");
        result.put("deadline", deadline.format(DAY));
        result.put("daysLeft", daysLeft);
        result.put("phases", phases);
        return result;
    }

    public Map<String, Object> apply(Long userId, Map<String, Object> body) {
        if (body == null) {
            throw new RuntimeException("请求体不能为空");
        }
        Long projectId = toLong(body.get("projectId"));
        String planBatchId = text(body.get("planBatchId"));
        Object phasesObj = body.get("phases");
        if (projectId == null) {
            throw new RuntimeException("项目 ID 不能为空");
        }
        if (!StringUtils.hasText(planBatchId)) {
            throw new RuntimeException("规划批次号不能为空");
        }
        List<Map<String, Object>> phases = normalizePhases(phasesObj, null);
        if (phases.isEmpty()) {
            throw new RuntimeException("规划阶段不能为空");
        }
        return wbTaskService.applyPlanBatch(userId, projectId, planBatchId, phases);
    }

    private String buildUserPrompt(WbProject project, String goal, String constraints,
                                   LocalDate deadline, long daysLeft, String paceHint) {
        StringBuilder sb = new StringBuilder();
        sb.append("项目名称：").append(project.getName()).append('\n');
        if (StringUtils.hasText(project.getDescription())) {
            sb.append("项目描述：").append(project.getDescription().trim()).append('\n');
        }
        sb.append("规划目标：").append(goal).append('\n');
        sb.append("项目截止时间：").append(deadline.format(DAY)).append('\n');
        sb.append("剩余天数：").append(daysLeft).append(" 天\n");
        sb.append("拆解节奏：").append(paceHint).append('\n');
        if (StringUtils.hasText(constraints)) {
            sb.append("其它约束：").append(constraints).append('\n');
        }
        sb.append("请按截止时间与剩余天数输出阶段与步骤规划 JSON（含 dueTime）。");
        return sb.toString();
    }

    private static String paceHint(long daysLeft) {
        if (daysLeft <= 14) {
            return "两周内交付：极简必做，阶段少、步骤狠，避免铺开。";
        }
        if (daysLeft <= 45) {
            return "约一到一个半月：均衡拆分，留一点缓冲。";
        }
        return "较长周期（约两个月或以上）：可细分阶段，但仍要全部落在截止日前。";
    }

    private LocalDate parseDeadline(Object value) {
        String raw = text(value);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        if (raw.length() >= 10) {
            raw = raw.substring(0, 10);
        }
        try {
            return LocalDate.parse(raw, DAY);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("项目截止时间格式无效，请使用 yyyy-MM-dd");
        }
    }

    private Map<String, Object> parsePlanJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("AI 未返回有效规划结果");
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("解析规划 JSON 失败: {}", e.getMessage());
            throw new RuntimeException("AI 返回格式无效，请重试");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizePhases(Object phasesObj, LocalDate deadlineCap) {
        List<Map<String, Object>> cleaned = new ArrayList<>();
        if (!(phasesObj instanceof List<?> list)) {
            return cleaned;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> phase = (Map<String, Object>) raw;
            String title = text(phase.get("title"));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            Map<String, Object> out = new HashMap<>();
            out.put("title", title);
            out.put("description", text(phase.get("description")));
            out.put("dueTime", clampDue(text(phase.get("dueTime")), deadlineCap));
            List<Map<String, Object>> steps = new ArrayList<>();
            Object stepsObj = phase.get("steps");
            if (stepsObj instanceof List<?> stepList) {
                for (Object s : stepList) {
                    if (!(s instanceof Map<?, ?> stepRaw)) {
                        continue;
                    }
                    Map<String, Object> step = (Map<String, Object>) stepRaw;
                    String stepTitle = text(step.get("title"));
                    if (!StringUtils.hasText(stepTitle)) {
                        continue;
                    }
                    Map<String, Object> stepOut = new HashMap<>();
                    stepOut.put("title", stepTitle);
                    stepOut.put("description", text(step.get("description")));
                    stepOut.put("priority", step.get("priority"));
                    stepOut.put("status", text(step.get("status")));
                    stepOut.put("dueTime", clampDue(text(step.get("dueTime")), deadlineCap));
                    steps.add(stepOut);
                }
            }
            out.put("steps", steps);
            cleaned.add(out);
        }
        return cleaned;
    }

    private String clampDue(String due, LocalDate deadlineCap) {
        if (!StringUtils.hasText(due)) {
            return "";
        }
        String day = due.length() >= 10 ? due.substring(0, 10) : due;
        try {
            LocalDate d = LocalDate.parse(day, DAY);
            if (deadlineCap != null && d.isAfter(deadlineCap)) {
                d = deadlineCap;
            }
            return d.format(DAY);
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    private WbProject requireOwnedProject(Long userId, Long projectId) {
        WbProject project = wbProjectService.getOne(
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getProjectId, projectId)
                        .eq(WbProject::getUserId, userId)
        );
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        return project;
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
