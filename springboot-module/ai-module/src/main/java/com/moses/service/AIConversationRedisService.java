package com.moses.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.constant.AIConstants;
import com.moses.model.AIConversationMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIConversationRedisService {

    private static final Duration CONVERSATION_TTL = Duration.ofDays(AIConstants.CONVERSATION_TTL_DAYS);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMemory chatMemory;

    public AIConversationRedisService(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      ChatMemory chatMemory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.chatMemory = chatMemory;
    }

    public String createConversation(Long userId, String title) {
        if (userId == null) {
            return null;
        }
        String conversationId = UUID.randomUUID().toString().replace("-", "");
        saveSessionMeta(userId, conversationId, StringUtils.hasText(title) ? title.trim() : AIConstants.DEFAULT_TITLE, LocalDateTime.now());
        return conversationId;
    }

    public List<AIConversationMeta> listConversations(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Set<String> ids = stringRedisTemplate.opsForSet().members(buildSessionSetKey(userId));
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = ids.stream()
                .filter(StringUtils::hasText)
                .map(id -> buildSessionMetaKey(userId, id))
                .collect(Collectors.toList());

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        List<AIConversationMeta> sessions = new ArrayList<>();
        for (Object raw : results) {
            if (raw instanceof String json) {
                try {
                    AIConversationMeta meta = objectMapper.readValue(json, AIConversationMeta.class);
                    if (meta != null && StringUtils.hasText(meta.getConversationId())) {
                        sessions.add(meta);
                    }
                } catch (JsonProcessingException e) {
                    log.warn("解析会话元数据失败, json={}", json, e);
                }
            }
        }

        sessions.sort(Comparator.comparing(AIConversationMeta::getLastActiveAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }

    public void deleteConversation(Long userId, String conversationId) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        String id = conversationId.trim();
        chatMemory.clear(id);
        stringRedisTemplate.delete(buildSessionMetaKey(userId, id));
        stringRedisTemplate.opsForSet().remove(buildSessionSetKey(userId), id);
    }

    public String ensureConversation(Long userId, String conversationId, String firstMessage) {
        if (!StringUtils.hasText(conversationId)) {
            return createConversation(userId, generateTitle(firstMessage));
        }
        String id = conversationId.trim();
        AIConversationMeta existing = getSessionMeta(userId, id);
        if (existing == null) {
            saveSessionMeta(userId, id, generateTitle(firstMessage), LocalDateTime.now());
        }
        return id;
    }

    private void saveSessionMeta(Long userId, String conversationId, String title, LocalDateTime lastActiveAt) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return;
        }
        String id = conversationId.trim();
        AIConversationMeta sessionMeta = new AIConversationMeta();
        sessionMeta.setConversationId(id);
        sessionMeta.setTitle(StringUtils.hasText(title) ? title : AIConstants.DEFAULT_TITLE);
        sessionMeta.setLastActiveAt(lastActiveAt != null ? lastActiveAt : LocalDateTime.now());
        try {
            String metaJson = objectMapper.writeValueAsString(sessionMeta);
            stringRedisTemplate.opsForValue().set(buildSessionMetaKey(userId, id), metaJson, CONVERSATION_TTL);
            stringRedisTemplate.opsForSet().add(buildSessionSetKey(userId), id);
            stringRedisTemplate.expire(buildSessionSetKey(userId), CONVERSATION_TTL);
        } catch (JsonProcessingException e) {
            log.error("保存会话元数据失败, userId={}, conversationId={}", userId, conversationId, e);
        }
    }

    private AIConversationMeta getSessionMeta(Long userId, String conversationId) {
        if (userId == null || !StringUtils.hasText(conversationId)) {
            return null;
        }
        String raw = stringRedisTemplate.opsForValue().get(buildSessionMetaKey(userId, conversationId.trim()));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, AIConversationMeta.class);
        } catch (JsonProcessingException e) {
            log.warn("解析会话元数据失败, conversationId={}", conversationId, e);
            return null;
        }
    }

    private String generateTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return AIConstants.DEFAULT_TITLE;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() <= AIConstants.TITLE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, AIConstants.TITLE_MAX_LENGTH) + "...";
    }

    private String buildSessionSetKey(Long userId) {
        return AIConstants.sessionSetKey(userId);
    }

    private String buildSessionMetaKey(Long userId, String conversationId) {
        return AIConstants.sessionMetaKey(userId, conversationId);
    }
}
