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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作台 AI Tool（读看板 / 落板 / 撤销批次）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchTools {

    private final WbTaskService wbTaskService;
    private final ObjectMapper objectMapper;

    @Tool(description = "获取指定项目的看板快照：项目名称/描述、各状态任务数量与任务摘要。")
    public String getProjectBoard(@ToolParam(description = "项目 ID") Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return toJson(wbTaskService.getProjectBoardSnapshot(userId, projectId));
    }

    @Tool(description = "将规划阶段与步骤批量写入项目看板。阶段为父任务、步骤为子任务。仅在用户确认落板后调用。phasesJson 格式:[{\"title\":\"阶段\",\"description\":\"\",\"steps\":[{\"title\":\"步骤\",\"description\":\"\",\"priority\":1,\"status\":\"0\"}]}]")
    public String createProjectTasks(
            @ToolParam(description = "项目 ID") Long projectId,
            @ToolParam(description = "规划批次号；空则自动生成") String planBatchId,
            @ToolParam(description = "阶段列表 JSON") String phasesJson) {
        Long userId = StpUtil.getLoginIdAsLong();
        String batchId = StringUtils.hasText(planBatchId)
                ? planBatchId.trim()
                : UUID.randomUUID().toString().replace("-", "");
        return toJson(wbTaskService.applyPlanBatch(userId, projectId, batchId, parsePhases(phasesJson)));
    }

    @Tool(description = "按 planBatchId 撤销本用户某次 AI 规划落板产生的全部任务。")
    public String undoTaskPlan(@ToolParam(description = "规划批次号 planBatchId") String planBatchId) {
        Long userId = StpUtil.getLoginIdAsLong();
        int removed = wbTaskService.revokeByPlanBatchId(userId, planBatchId);
        Map<String, Object> result = new HashMap<>();
        result.put("removed", removed);
        result.put("planBatchId", planBatchId);
        return toJson(result);
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
