package com.moses.constant;

public final class AIConstants {

    private AIConstants() {
    }

    /** 项目命名空间，避免与共用 Redis 的其他服务串会话 */
    private static final String APP_PREFIX = "spine:";

    public static final String CHAT_MEMORY_KEY_PREFIX = APP_PREFIX + "ai:chat:memory:";

    public static final String SESSION_SET_PREFIX = APP_PREFIX + "ai:chat:sessions:";
    public static final String SESSION_META_PREFIX = APP_PREFIX + "ai:chat:session:meta:";

    public static String chatMemoryKey(String conversationId) {
        return CHAT_MEMORY_KEY_PREFIX + conversationId;
    }

    public static String sessionSetKey(Long userId) {
        return SESSION_SET_PREFIX + userId;
    }

    public static String sessionMetaKey(Long userId, String conversationId) {
        return SESSION_META_PREFIX + userId + ":" + conversationId.trim();
    }

    public static final int MAX_MESSAGES = 40;
    public static final int HISTORY_WINDOW = 20;
    public static final int TITLE_MAX_LENGTH = 20;

    public static final String DEFAULT_TITLE = "新对话";

    public static final long CONVERSATION_TTL_DAYS = 30;
}
