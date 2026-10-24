import request from '@/utils/request'
import type { AiStreamChunk, ApiResult } from '@/types/api'
import type { AiChatMessage, AIConversationMeta } from '@/types/entities'

/**
 * SSE 流式对话（axios fetch 适配器 + ReadableStream）
 * 消息经查询参数传递（本项目后端为 GET /ai/chat）
 */
export function chatWithAiStream(
  message: string,
  conversationId: string | undefined,
  onChunk: (chunk: string, conversationId?: string) => void,
  onComplete?: () => void | Promise<void>,
  onError?: (error: unknown) => void,
): () => void {
  const params: Record<string, string> = { message }
  if (conversationId) {
    params.conversationId = conversationId
  }

  const controller = new AbortController()

  request
    .get('/ai/chat', {
      params,
      headers: { Accept: 'text/event-stream' },
      adapter: 'fetch',
      responseType: 'stream',
      rawResponse: true,
      signal: controller.signal,
      timeout: 0
    })
    .then((response) => {
      if (response.status < 200 || response.status >= 300) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.data.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      function readStream(): Promise<void> {
        return reader.read().then(({ done, value }) => {
          if (done) {
            if (onComplete) {
              onComplete()
            }
            return
          }

          if (value) {
            buffer += decoder.decode(value, { stream: true })
          }

          const lines = buffer.split('\n')
          buffer = lines.pop() ?? ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              if (data) {
                try {
                  const parsed = JSON.parse(data) as AiStreamChunk
                  if (parsed.code === 1 && parsed.data) {
                    onChunk(parsed.data, parsed.conversationId)
                  }
                } catch (error) {
                  console.error('解析SSE数据失败:', error, data)
                }
              }
            }
          }

          return readStream()
        })
      }

      return readStream()
    })
    .catch((error: unknown) => {
      const err = error as { name?: string; code?: string }
      if (err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
        console.log('SSE请求已取消')
        return
      }
      console.error('SSE请求失败:', error)
      if (onError) {
        onError(error)
      }
    })

  return () => {
    controller.abort()
  }
}

export function listAiConversationsApi(): Promise<ApiResult<AIConversationMeta[]>> {
  return request.get<AIConversationMeta[]>('/ai/listConversations')
}

export function getAiHistoryApi(conversationId: string): Promise<ApiResult<AiChatMessage[]>> {
  return request.get<AiChatMessage[]>('/ai/getHistory', { params: { conversationId } })
}

export function deleteAiConversationApi(conversationId: string): Promise<ApiResult> {
  return request.delete('/ai/deleteConversation', { params: { conversationId } })
}

/** AI 规划阶段内的步骤项 */
export interface AiPlanPhaseStep {
  title?: string
  dueTime?: string | null
  description?: string | null
  [key: string]: unknown
}

/** AI 规划阶段预览项 */
export interface AiPlanPhase {
  title?: string
  dueTime?: string | null
  description?: string | null
  steps?: AiPlanPhaseStep[]
  [key: string]: unknown
}

/** AI 规划预览结果 */
export interface AiPlanPreview {
  planBatchId?: number | string
  deadline?: string
  daysLeft?: number
  summary?: string
  phases?: AiPlanPhase[]
  [key: string]: unknown
}

/** AI 规划落板结果 */
export interface AiPlanApplyResult {
  created?: number
  [key: string]: unknown
}

/** AI 规划预览（不写库） */
export function planPreviewApi(body: unknown): Promise<ApiResult<AiPlanPreview>> {
  return request.post('/ai/plan/preview', body)
}

/** AI 规划确认落板 */
export function planApplyApi(body: unknown): Promise<ApiResult<AiPlanApplyResult>> {
  return request.post('/ai/plan/apply', body)
}
