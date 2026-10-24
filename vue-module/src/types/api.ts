/** 接口协议通用类型：与后端 ResultConfig / ErrorCode 一一对应 */

/** 与后端 ErrorCode 枚举一一对应，稳定编码，勿改 */
export type ApiErrorCode =
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'USER_NOT_FOUND'
  | 'USER_BANNED'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'BAD_REQUEST'
  | 'SYSTEM_ERROR'

/** 与后端 ResultConfig 对应 */
export interface ApiResult<T = unknown> {
  code: 0 | 1
  msg: string
  data?: T
  errorCode?: ApiErrorCode
}

/** MyBatis-Plus 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 带稳定错误编码的业务错误（request 层统一抛出） */
export class ApiError extends Error {
  errorCode?: ApiErrorCode

  constructor(message: string, errorCode?: ApiErrorCode) {
    super(message)
    this.name = 'ApiError'
    this.errorCode = errorCode
  }
}

/** SSE 流式响应的单条消息 */
export interface AiStreamChunk {
  code?: number
  data?: string
  conversationId?: string
}

/** 从任意异常提取可展示文案 */
export function toErrorMessage(error: unknown, fallback = ''): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'object' && error !== null && 'message' in error) {
    const msg = (error as { message?: unknown }).message
    if (typeof msg === 'string' && msg) return msg
  }
  return fallback
}
