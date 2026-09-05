package com.moses.tool;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.service.WbTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作台 AI Tool（读看板 / 落板）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchTools {

    private final WbTaskService wbTaskService;
    private final ObjectMapper objectMapper;

    @Tool(description = "获取指定顶层任务的看板快照：名称/描述、各状态子任务数量与摘要。projectTaskId 为 node_type=project 的顶层任务 ID。")
    public String getProjectBoard(@ToolParam(description = "顶层任务 ID") Long projectTaskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return toJson(wbTaskService.getProjectBoardSnapshot(userId, projectTaskId));
    }

    @Tool(description = "将规划阶段与步骤批量写入任务树。阶段为 phase 子任务、步骤为 task 孙任务。仅在用户确认落板后调用。phasesJson 格式:[{\"title\":\"阶段\",\"description\":\"\",\"steps\":[{\"title\":\"步骤\",\"description\":\"\",\"priority\":1,\"status\":\"0\"}]}]")
    public String createProjectTasks(
            @ToolParam(description = "顶层任务 ID") Long projectTaskId,
            @ToolParam(description = "规划批次号；空则自动生成") String planBatchId,
            @ToolParam(description = "阶段列表 JSON") String phasesJson) {
        Long userId = StpUtil.getLoginIdAsLong();
        String batchId = StringUtils.hasText(planBatchId)
                ? planBatchId.trim()
                : UUID.randomUUID().toString().replace("-", "");
        return toJson(wbTaskService.applyPlanBatch(userId, projectTaskId, batchId, parsePhases(phasesJson)));
    }

    private List<Map<String, Object>> parsePhases(String phasesJson) {
        if (!StringUtils.hasText(phasesJson)) {
            throw new RuntimeException("阶段列表不能为空");
        }
        try {
            List<Map<String, Object>> phases = objectMapper.readValue(
                    phasesJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
            return phases != null ? phases : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.warn("解析规划阶段 JSON 失败: {}", e.getMessage());
            throw new RuntimeException("阶段列表 JSON 格式无效");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
