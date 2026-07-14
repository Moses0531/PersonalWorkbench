package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.moses.model.AIConversationMeta;
import com.moses.service.AIChatService;
import com.moses.service.AIConversationRedisService;
import com.moses.config.ResultConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Tag(name = "Ai对话", description = "Ai助手对话相关接口")
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final AIChatService chatService;
    private final AIConversationRedisService conversationRedisService;

    public AiController(AIChatService chatService,
                        AIConversationRedisService conversationRedisService) {
        this.chatService = chatService;
        this.conversationRedisService = conversationRedisService;
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

    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话列表")
    @GetMapping("/listConversations")
    public ResultConfig listConversations() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        List<AIConversationMeta> conversations = conversationRedisService.listConversations(userId);
        return ResultConfig.success(conversations);
    }

    @Operation(summary = "获取历史消息", description = "获取指定会话的历史对话消息")
    @GetMapping("/getHistory")
    public ResultConfig getHistory(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        StpUtil.checkLogin();
        List<Map<String, String>> messages = chatService.getHistory(conversationId);
        return ResultConfig.success(messages);
    }

    @Operation(summary = "删除会话", description = "删除指定会话及其所有历史消息")
    @DeleteMapping("/deleteConversation")
    public ResultConfig deleteConversation(
            @Parameter(description = "会话ID") @RequestParam(name = "conversationId") String conversationId) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        conversationRedisService.deleteConversation(userId, conversationId);
        return ResultConfig.success();
    }
}
