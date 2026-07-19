package com.moses.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AIConversationRedisService conversationRedisService;
    private final ObjectMapper objectMapper;

    @Value("${ai.system-prompt:你是一个AI助手。}")
    private String systemPrompt;

    public AIChatService(ChatClient chatClient,
                         ChatMemory chatMemory,
                         AIConversationRedisService conversationRedisService,
                         ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.conversationRedisService = conversationRedisService;
        this.objectMapper = objectMapper;
    }

    public Flux<String> chat(Long userId, String conversationId, String message) {
        String actualConversationId = conversationRedisService.ensureConversation(userId, conversationId, message);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, actualConversationId))
                .stream()
                .content()
                .map(chunk -> buildJsonResponse(chunk, actualConversationId))
                .doOnError(error ->
                        log.error("AI对话出错, userId={}, conversationId={}, message={}", userId, actualConversationId, message, error)
                );
    }

    public List<Map<String, String>> getHistory(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        return messages.stream()
                .map(msg -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", msg.getMessageType().getValue());
                    map.put("content", msg.getText());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String buildJsonResponse(String data, String conversationId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 1);
            response.put("msg", "success");
            response.put("data", data);
            response.put("conversationId", conversationId);
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败, data={}", data, e);
            return "{\"code\":0,\"msg\":\"序列化失败\",\"data\":\"\"}";
        }
    }
}
