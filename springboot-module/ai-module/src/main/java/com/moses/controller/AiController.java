package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.moses.config.ResultConfig;
import com.moses.model.AIConversationMeta;
import com.moses.service.AIChatService;
import com.moses.service.AIConversationRedisService;
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

@Tag(name = "Ai对话", description = "Ai助手对话与任务智能规划")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final AIChatService chatService;
    private final AIConversationRedisService conversationRedisService;
    private final AiPlanService aiPlanService;

    public AiController(AIChatService chatService,
                        AIConversationRedisService conversationRedisService,
                        AiPlanService aiPlanService) {
        this.chatService = chatService;
        this.conversationRedisService = conversationRedisService;
        this.aiPlanService = aiPlanService;
    }

    @Operation(summary = "Ai对话（流式）", description = "发送消息并接收流式响应；权限 ai:chat 可单独关闭入口与接口")
    @SaCheckPermission("ai:chat")
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

        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.chat(userId, conversationId, message);
    }

    @Operation(summary = "获取会话列表")
    @SaCheckPermission("ai:chat")
    @GetMapping("/listConversations")
    public ResultConfig listConversations() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AIConversationMeta> conversations = conversationRedisService.listConversations(userId);
        return ResultConfig.success(conversations);
    }

    @Operation(summary = "获取历史消息")
    @SaCheckPermission("ai:chat")
    @GetMapping("/getHistory")
    public ResultConfig getHistory(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        List<Map<String, String>> messages = chatService.getHistory(conversationId);
        return ResultConfig.success(messages);
    }

    @Operation(summary = "删除会话")
    @SaCheckPermission("ai:chat")
    @DeleteMapping("/deleteConversation")
    public ResultConfig deleteConversation(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        conversationRedisService.deleteConversation(userId, conversationId);
        return ResultConfig.success();
    }

    @PostMapping("/plan/preview")
    @Operation(summary = "AI 规划预览（不写库）；权限 ai:plan 可单独关闭")
    @SaCheckPermission("ai:plan")
    public ResultConfig planPreview(@RequestBody Map<String, Object> body) {
        return ResultConfig.success(aiPlanService.preview(StpUtil.getLoginIdAsLong(), body));
    }

    @PostMapping("/plan/apply")
    @Operation(summary = "AI 规划确认落板；权限 ai:plan 可单独关闭")
    @SaCheckPermission("ai:plan")
    public ResultConfig planApply(@RequestBody Map<String, Object> body) {
        return ResultConfig.success(aiPlanService.apply(StpUtil.getLoginIdAsLong(), body));
    }
}
