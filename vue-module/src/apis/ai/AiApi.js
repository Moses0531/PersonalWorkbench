import { http } from '../common/http'

/** Ai-module AiController */
export function chatWithAiApi(message, conversationId) {
  return http.get('/ai/chat', { params: { message, conversationId } })
}

/**
 * SSE 流式对话（axios fetch 适配器 + ReadableStream）
 */
export function chatWithAiStream(message, conversationId, onChunk, onComplete, onError) {
  const params = { message }
  if (conversationId) {
    params.conversationId = conversationId
  }

  const controller = new AbortController()

  http
    .get('/ai/chat', {
      params,
      headers: { Accept: 'text/event-stream' },
      adapter: 'fetch',
      responseType: 'stream',
      rawResponse: true,
      signal: controller.signal
    })
    .then((response) => {
      if (response.status < 200 || response.status >= 300) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.data.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      function readStream() {
        return reader.read().then(({ done, value }) => {
          if (done) {
            if (onComplete) {
              onComplete()
            }
            return
          }

          buffer += decoder.decode(value, { stream: true })

          const lines = buffer.split('\n')
          buffer = lines.pop()

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              if (data) {
                try {
                  const parsed = JSON.parse(data)
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
    .catch((error) => {
      if (error.name === 'AbortError' || error.code === 'ERR_CANCELED') {
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

export function listAiConversationsApi() {
  return http.get('/ai/listConversations')
}

export function getAiHistoryApi(conversationId) {
  return http.get('/ai/getHistory', { params: { conversationId } })
}

export function deleteAiConversationApi(conversationId) {
  return http.delete('/ai/deleteConversation', { params: { conversationId } })
}
