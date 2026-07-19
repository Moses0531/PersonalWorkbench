package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.moses.config.ResultConfig;
import com.moses.model.AIConversationMeta;
import com.moses.service.AIChatService;
import com.moses.service.AIConversationRedisService;
import com.moses.service.AiMeetingSummaryService;
import com.moses.service.AiPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Tag(name = "Ai对话", description = "Ai助手对话、项目智能规划与会议整理")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final AIChatService chatService;
    private final AIConversationRedisService conversationRedisService;
    private final AiPlanService aiPlanService;
    private final AiMeetingSummaryService aiMeetingSummaryService;

    public AiController(AIChatService chatService,
                        AIConversationRedisService conversationRedisService,
                        AiPlanService aiPlanService,
                        AiMeetingSummaryService aiMeetingSummaryService) {
        this.chatService = chatService;
        this.conversationRedisService = conversationRedisService;
        this.aiPlanService = aiPlanService;
        this.aiMeetingSummaryService = aiMeetingSummaryService;
    }

    @Operation(summary = "Ai对话（流式）", description = "发送消息并接收流式响应")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @Parameter(description = "用户消息内容") @RequestParam(name = "message") String message,
            @Parameter(description = "会话ID，不传则创建新会话") @RequestParam(name = "conversationId", required = false) String conversationId) {

        if (!StringUtils.hasText(message)) {
            return Flux.error(new IllegalArgumentException("消息不能为空"));
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return Flux.error(new IllegalArgumentException("消息长度不能超过" + MAX_MESSAGE_LENGTH + "字符"));
        }

        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();

        return chatService.chat(userId, conversationId, message);
    }

    @Operation(summary = "获取会话列表")
    @GetMapping("/listConversations")
    public ResultConfig listConversations() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        List<AIConversationMeta> conversations = conversationRedisService.listConversations(userId);
        return ResultConfig.success(conversations);
    }

    @Operation(summary = "获取历史消息")
    @GetMapping("/getHistory")
    public ResultConfig getHistory(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        StpUtil.checkLogin();
        List<Map<String, String>> messages = chatService.getHistory(conversationId);
        return ResultConfig.success(messages);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/deleteConversation")
    public ResultConfig deleteConversation(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        conversationRedisService.deleteConversation(userId, conversationId);
        return ResultConfig.success();
    }

    @PostMapping("/plan/preview")
    @Operation(summary = "AI 规划预览（不写库）")
    @SaCheckPermission("task:add")
    public ResultConfig planPreview(@RequestBody Map<String, Object> body) {
        return ResultConfig.success(aiPlanService.preview(StpUtil.getLoginIdAsLong(), body));
    }

    @PostMapping("/plan/apply")
    @Operation(summary = "AI 规划确认落板")
    @SaCheckPermission("task:add")
    public ResultConfig planApply(@RequestBody Map<String, Object> body) {
        return ResultConfig.success(aiPlanService.apply(StpUtil.getLoginIdAsLong(), body));
    }

    @PostMapping("/meeting/summary")
    @Operation(summary = "AI 会议整理（根据材料生成概要并写回）")
    @SaCheckPermission("ai:meeting:summary")
    public ResultConfig meetingSummary(@RequestBody Map<String, Object> body) {
        Long meetingId = null;
        if (body != null && body.get("meetingId") != null) {
            meetingId = Long.valueOf(String.valueOf(body.get("meetingId")));
        }
        return ResultConfig.success(
                aiMeetingSummaryService.summarize(StpUtil.getLoginIdAsLong(), meetingId)
        );
    }
}
