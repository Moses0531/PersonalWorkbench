package com.moses.repository;

import com.moses.constant.AIConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class RedisChatMemory implements ChatMemory {

    private static final Duration CONVERSATION_TTL = Duration.ofDays(AIConstants.CONVERSATION_TTL_DAYS);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisChatMemory(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }
        String key = buildKey(conversationId);
        try {
            for (Message message : messages) {
                String serialized = serializeMessage(message);
                stringRedisTemplate.opsForList().rightPush(key, serialized);
            }
            stringRedisTemplate.expire(key, CONVERSATION_TTL);
            stringRedisTemplate.opsForList().trim(key, -AIConstants.MAX_MESSAGES, -1);
        } catch (Exception e) {
            log.error("保存聊天消息失败, conversationId={}", conversationId, e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        String key = buildKey(conversationId);
        try {
            List<String> rawList = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (rawList == null || rawList.isEmpty()) {
                return List.of();
            }
            List<Message> messages = new ArrayList<>();
            for (String raw : rawList) {
                Message message = deserializeMessage(raw);
                if (message != null) {
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            log.error("获取聊天消息失败, conversationId={}", conversationId, e);
            return List.of();
        }
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }
        stringRedisTemplate.delete(buildKey(conversationId));
    }

    private String buildKey(String conversationId) {
        return AIConstants.chatMemoryKey(conversationId);
    }

    private String serializeMessage(Message message) {
        if (message instanceof UserMessage) {
            return "USER:" + message.getText();
        } else if (message instanceof AssistantMessage) {
            return "ASSISTANT:" + message.getText();
        }
        return "UNKNOWN:" + message.getText();
    }

    private Message deserializeMessage(String raw) {
        if (raw == null || !raw.contains(":")) {
            return null;
        }
        int colonIndex = raw.indexOf(':');
        String role = raw.substring(0, colonIndex);
        String content = raw.substring(colonIndex + 1);
        switch (role) {
            case "USER":
                return new UserMessage(content);
            case "ASSISTANT":
                return new AssistantMessage(content);
            default:
                return null;
        }
    }
}
