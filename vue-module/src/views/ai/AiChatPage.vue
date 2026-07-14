<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import {
  chatWithAiStream,
  deleteAiConversationApi,
  getAiHistoryApi,
  listAiConversationsApi
} from '@/apis/ai/AiApi'

const loading = ref(false)
const clearLoading = ref(false)
const message = ref('')
const messages = ref([])
const conversations = ref([])
const activeConversationId = ref('')
const listRef = ref(null)
const streamingMessageIndex = ref(-1)
const cancelStream = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (!listRef.value) return
    listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

async function loadConversations() {
  try {
    const result = await listAiConversationsApi()
    conversations.value = Array.isArray(result?.data) ? result.data : []
    if (!activeConversationId.value && conversations.value.length > 0) {
      activeConversationId.value = conversations.value[0].conversationId
    }
  } catch (_err) {
    conversations.value = []
  }
}

async function loadHistory() {
  if (!activeConversationId.value) {
    messages.value = []
    return
  }
  try {
    const result = await getAiHistoryApi(activeConversationId.value)
    messages.value = Array.isArray(result?.data) ? result.data : []
  } catch (_err) {
    messages.value = []
  }
  scrollToBottom()
}

async function selectConversation(conversationId) {
  if (!conversationId || activeConversationId.value === conversationId) return
  activeConversationId.value = conversationId
  await loadHistory()
}

async function createConversation() {
  activeConversationId.value = ''
  messages.value = []
  message.value = ''
}

async function refreshConversationsAndHistory(preferredConversationId = '') {
  await loadConversations()
  if (preferredConversationId) {
    activeConversationId.value = preferredConversationId
  }
  if (!activeConversationId.value && conversations.value.length > 0) {
    activeConversationId.value = conversations.value[0].conversationId
  }
  await loadHistory()
}

async function sendMessage() {
  const content = message.value.trim()
  if (!content || loading.value) return

  loading.value = true
  const draft = content
  message.value = ''

  messages.value.push({
    role: 'user',
    content: draft,
    timestamp: new Date().toISOString()
  })

  const assistantMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    timestamp: new Date().toISOString()
  })
  streamingMessageIndex.value = assistantMessageIndex
  scrollToBottom()

  try {
    cancelStream.value = chatWithAiStream(
        draft,
        activeConversationId.value,
        (chunk, conversationId) => {
          if (conversationId && !activeConversationId.value) {
            activeConversationId.value = conversationId
          }
          if (streamingMessageIndex.value >= 0 && streamingMessageIndex.value < messages.value.length) {
            messages.value[streamingMessageIndex.value].content += chunk
            scrollToBottom()
          }
        },
        async () => {
          streamingMessageIndex.value = -1
          cancelStream.value = null
          await loadConversations()
          loading.value = false
          scrollToBottom()
        },
        (error) => {
          antMessage.error(error?.message || '发送失败')
          if (streamingMessageIndex.value >= 0 && streamingMessageIndex.value < messages.value.length) {
            if (!messages.value[streamingMessageIndex.value].content) {
              messages.value[streamingMessageIndex.value].content = '抱歉，本次回答失败，请稍后重试。'
            }
          }
          streamingMessageIndex.value = -1
          cancelStream.value = null
          loading.value = false
          scrollToBottom()
        }
    )
  } catch (err) {
    antMessage.error(err?.message || '发送失败')
    messages.value.push({
      role: 'assistant',
      content: '抱歉，本次回答失败，请稍后重试。',
      timestamp: new Date().toISOString()
    })
    loading.value = false
    scrollToBottom()
  }
}

async function clearHistory() {
  if (!activeConversationId.value || clearLoading.value) return
  clearLoading.value = true
  try {
    await deleteAiConversationApi(activeConversationId.value)
    activeConversationId.value = ''
    messages.value = []
    await refreshConversationsAndHistory()
    antMessage.success('对话已删除')
  } catch (err) {
    antMessage.error(err?.message || '删除失败')
  } finally {
    clearLoading.value = false
  }
}

onUnmounted(() => {
  if (cancelStream.value) {
    cancelStream.value()
    cancelStream.value = null
  }
})

onMounted(async () => {
  await refreshConversationsAndHistory()
})
</script>

<template>
  <div class="chat-page">
    <!-- 背景装饰 -->
    <div class="bg-blob bg-blob--1"></div>
    <div class="bg-blob bg-blob--2"></div>

    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <h3 class="sidebar-title">对话列表</h3>
          <button class="btn-new" @click="createConversation" title="新建会话">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </button>
        </div>

        <div class="conversation-list">
          <div
              v-for="item in conversations"
              :key="item.conversationId"
              class="conversation-item"
              :class="{ active: item.conversationId === activeConversationId }"
              @click="selectConversation(item.conversationId)"
          >
            <span class="conversation-title">{{ item.title || '新对话' }}</span>
          </div>

          <div v-if="conversations.length === 0" class="conversation-empty">
            <span>暂无对话</span>
          </div>
        </div>
      </aside>

      <!-- 右侧聊天主区域 -->
      <main class="chat-main">
        <!-- 顶部栏 -->
        <div class="chat-topbar">
          <div class="topbar-info">
            <div>
              <span class="topbar-name">AI 助手</span>
              <span class="topbar-status">
                <span class="status-dot"></span>
                在线
              </span>
            </div>
          </div>
          <div class="topbar-actions">
            <button
                class="btn-delete"
                :disabled="!activeConversationId || clearLoading"
                @click="clearHistory"
                title="删除当前会话"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="listRef" class="chat-list">
          <!-- 空状态 -->
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="empty-illustration">
              <svg viewBox="0 0 120 120" class="empty-svg">
                <circle cx="60" cy="55" r="36" fill="var(--color-accent-soft)" />
                <circle cx="60" cy="55" r="28" fill="var(--color-surface)" stroke="var(--color-accent)" stroke-width="1" opacity="0.8" />
                <path d="M48 50 L52 56 L72 42" fill="none" stroke="var(--color-accent)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
                <circle cx="30" cy="30" r="2" fill="var(--color-accent)" opacity="0.2" class="empty-dot empty-dot--1" />
                <circle cx="90" cy="25" r="2.5" fill="var(--color-accent)" opacity="0.15" class="empty-dot empty-dot--2" />
                <circle cx="95" cy="70" r="1.5" fill="var(--color-accent)" opacity="0.2" class="empty-dot empty-dot--3" />
                <circle cx="25" cy="75" r="2" fill="var(--color-accent)" opacity="0.18" class="empty-dot empty-dot--4" />
              </svg>
            </div>
            <p class="empty-title">开始和 AI 对话</p>
            <p class="empty-desc">输入你的问题，AI 助手将为你解答</p>
          </div>

          <!-- 消息气泡 -->
          <div
              v-for="(item, idx) in messages"
              :key="`${idx}-${item.timestamp || ''}`"
              class="chat-row"
              :class="[item.role, { 'is-streaming': idx === streamingMessageIndex }]"
          >
            <div class="bubble-wrap">
              <div class="bubble" :class="`bubble--${item.role}`">
                <div class="bubble-content">{{ item.content }}<span v-if="idx === streamingMessageIndex" class="typing-cursor"></span></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-wrap">
            <a-textarea
                v-model:value="message"
                :rows="2"
                :auto-size="{ minRows: 2, maxRows: 6 }"
                placeholder="输入你的问题，按 Enter 发送..."
                @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-actions">
              <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
              <button
                  class="btn-send"
                  :class="{ 'is-loading': loading }"
                  :disabled="!message.trim() || loading"
                  @click="sendMessage"
              >
                <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
                <span v-if="loading" class="send-spinner"></span>
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  position: relative;
  padding: 24px;
  min-height: 100vh;
  background: var(--color-bg);
  font-family: var(--font-family-sans);
  overflow: hidden;
}

/* ── 背景 ── */
.bg-blob { position: fixed; border-radius: 50%; filter: blur(100px); pointer-events: none; opacity: 0.4; }
.bg-blob--1 { width: 500px; height: 500px; top: -10%; right: -8%; background: rgba(224,122,79,0.07); animation: blobFloat 20s ease-in-out infinite; }
.bg-blob--2 { width: 400px; height: 400px; bottom: -10%; left: -5%; background: rgba(190,160,120,0.06); animation: blobFloat 24s 4s ease-in-out infinite reverse; }
@keyframes blobFloat { 0%, 100% { transform: translate(0,0) scale(1); } 50% { transform: translate(30px,-20px) scale(1.05); } }

/* ── 主容器 ── */
.chat-container {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 0;
  height: calc(100vh - 48px);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* ── 左侧栏 ── */
.sidebar {
  background: var(--color-surface-elevated);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px 14px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.btn-new {
  width: var(--button-height-sm);
  height: var(--button-height-sm);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-normal);
}
.btn-new:hover {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
  transform: scale(1.05);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) var(--space-2);
}
.conversation-list::-webkit-scrollbar { width: 4px; }
.conversation-list::-webkit-scrollbar-thumb { background: rgba(107,94,82,0.12); border-radius: var(--radius-xs); }

.conversation-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  margin-bottom: var(--space-1);
  border: 1px solid transparent;
}
.conversation-item:hover {
  background: rgba(255,255,255,0.7);
}
.conversation-item.active {
  background: var(--color-surface);
  border-color: var(--color-accent-glow);
  box-shadow: 0 1px 4px rgba(224,122,79,0.08);
}

.conversation-title {
  font-size: var(--text-md);
  color: var(--color-text-body);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  font-weight: var(--font-medium);
}
.conversation-item.active .conversation-title {
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
}

.conversation-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-12) var(--space-4);
  color: var(--color-text-dim);
  font-size: var(--text-md);
}
/* ── 右侧主区域 ── */
.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 顶部栏 */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}

.topbar-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.topbar-name {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  display: block;
}

.topbar-status {
  font-size: var(--text-sm);
  color: var(--color-green);
  font-weight: var(--font-semibold);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: 1px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-green);
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 2px var(--color-green-soft); }
  50% { box-shadow: 0 0 0 5px rgba(74,186,106,0.05); }
}

.topbar-actions { display: flex; gap: var(--space-2); }

.btn-delete {
  width: var(--button-height-sm);
  height: var(--button-height-sm);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-normal);
}
.btn-delete:hover:not(:disabled) {
  color: #e05545;
  border-color: rgba(224,85,69,0.2);
  background: rgba(224,85,69,0.06);
}
.btn-delete:disabled { opacity: 0.35; cursor: not-allowed; }

/* ── 消息列表 ── */
.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  scroll-behavior: smooth;
}
.chat-list::-webkit-scrollbar { width: 5px; }
.chat-list::-webkit-scrollbar-thumb { background: rgba(107,94,82,0.1); border-radius: var(--radius-sm); }

/* 空状态 */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 4px;
}

.empty-illustration { margin-bottom: var(--space-2); }
.empty-svg { width: 100px; height: 100px; }

.empty-dot--1 { animation: dotFloat 4s ease-in-out infinite; }
.empty-dot--2 { animation: dotFloat 5s 1s ease-in-out infinite; }
.empty-dot--3 { animation: dotFloat 4.5s 2s ease-in-out infinite; }
.empty-dot--4 { animation: dotFloat 5.5s 0.5s ease-in-out infinite; }
@keyframes dotFloat { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }

.empty-title {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}
.empty-desc {
  margin: 0;
  font-size: var(--text-md);
  color: var(--color-text-dim);
}

/* 消息行 */
.chat-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: var(--space-5);
  animation: msgIn var(--transition-spring) forwards;
}
@keyframes msgIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.chat-row.user { justify-content: flex-end; }
.chat-row.assistant { justify-content: flex-start; }

/* 气泡 */
.bubble-wrap { max-width: 72%; min-width: 0; }

.bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-xl);
  line-height: var(--leading-relaxed);
  word-break: break-word;
  font-size: var(--text-lg);
  position: relative;
}

.bubble--assistant {
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  border-top-left-radius: var(--radius-xs);
}

.bubble--user {
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-deep));
  color: #fff;
  border-top-right-radius: var(--radius-xs);
  box-shadow: var(--shadow-accent);
}

.bubble-content {
  white-space: pre-wrap;
}

.is-streaming .bubble--assistant {
  border-color: var(--color-accent-glow);
}

/* 打字光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--color-accent);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursorBlink 0.8s ease-in-out infinite;
}
@keyframes cursorBlink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

/* ── 输入区域 ── */
.chat-input-area {
  padding: var(--space-4) var(--space-6) var(--space-5);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}

.input-wrap {
  position: relative;
  background: var(--color-surface-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.input-wrap:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px var(--color-accent-soft);
}

.input-wrap :deep(.ant-input) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: var(--space-3) var(--space-4) var(--space-1);
  font-size: var(--text-lg);
  color: var(--color-text-primary);
  font-family: inherit;
  line-height: var(--leading-relaxed);
  resize: none;
}
.input-wrap :deep(.ant-input::placeholder) {
  color: var(--color-text-dim);
}
.input-wrap :deep(.ant-input:focus) {
  box-shadow: none;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-1) var(--space-3) var(--space-2);
}

.input-hint {
  font-size: var(--text-sm);
  color: var(--color-text-dim);
  opacity: 0.7;
}

.btn-send {
  width: var(--button-height-md);
  height: var(--button-height-md);
  border-radius: var(--radius-lg);
  border: none;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-spring);
  box-shadow: var(--shadow-accent);
  flex-shrink: 0;
}
.btn-send:hover:not(:disabled) {
  background: var(--color-accent-deep);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(224,122,79,0.35);
}
.btn-send:active:not(:disabled) { transform: translateY(0); }
.btn-send:disabled { opacity: 0.4; cursor: not-allowed; }

.send-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 响应式 ── */
@media (max-width: 768px) {
  .chat-page { padding: var(--space-3); }
  .chat-container {
    grid-template-columns: 1fr;
    height: calc(100vh - 24px);
    border-radius: var(--radius-xl);
  }
  .sidebar {
    display: none;
  }
  .chat-list { padding: var(--space-4); }
  .chat-input-area { padding: var(--space-3) var(--space-4) var(--space-4); }
}
</style>
