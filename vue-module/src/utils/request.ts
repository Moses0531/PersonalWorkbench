import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ApiError, type ApiErrorCode, type ApiResult } from '@/types/api'

const TOKEN_KEY = 'rbac_token'

/** 会话失效类错误：清登录态并跳转登录页 */
const AUTH_EXPIRED_CODES = new Set<ApiErrorCode>(['UNAUTHORIZED', 'USER_BANNED'])

/** 按标签页隔离登录态，避免多标签共用同一账号 */
function getAuthToken(): string {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

function setAuthToken(token: string): void {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

// 清理旧版 localStorage 共享 token，防止与标签页隔离逻辑冲突
localStorage.removeItem(TOKEN_KEY)

function rejectWithCode(message: string, errorCode?: ApiErrorCode): ApiError {
  return new ApiError(message, errorCode)
}

let authRedirecting = false

/** 未登录 / 封禁时统一清态跳转，避免各页面各自比对文案 */
async function handleAuthExpired(errorCode?: ApiErrorCode): Promise<void> {
  if (!errorCode || !AUTH_EXPIRED_CODES.has(errorCode) || authRedirecting) {
    return
  }
  authRedirecting = true
  try {
    const [{ useUserStore }, { default: router }, { resetDynamicRoutes }] = await Promise.all([
      import('@/stores/userStore'),
      import('@/router'),
      import('@/router/dynamicRoutes'),
    ])
    const userStore = useUserStore()
    userStore.clearAuth()
    resetDynamicRoutes(router)
    const current = router.currentRoute.value
    if (current.path !== '/auth') {
      await router.replace({
        path: '/auth',
        // reason 供登录页区分提示：banned=账号被封禁，其余视为会话失效
        query: { redirect: current.fullPath, reason: errorCode === 'USER_BANNED' ? 'banned' : 'expired' },
      })
    }
  } catch {
    setAuthToken('')
    if (window.location.pathname !== '/auth') {
      window.location.href = '/auth'
    }
  } finally {
    authRedirecting = false
  }
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers[TOKEN_KEY] = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    // SSE / 流式等场景需要完整响应（含 status、ReadableStream）
    if (response.config.rawResponse) {
      return response
    }
    const result = response.data as ApiResult
    if (result?.code !== 1) {
      const errorCode = result?.errorCode
      handleAuthExpired(errorCode)
      return Promise.reject(rejectWithCode(result?.msg || '请求失败', errorCode))
    }
    // 业务层统一解包为 ApiResult（见 HttpClient 声明），此处仅为满足 axios 拦截器签名
    return result as unknown as AxiosResponse
  },
  (error) => {
    const errorCode: ApiErrorCode | undefined = error.response?.data?.errorCode
    const message =
      error.response?.data?.msg ||
      error.message ||
      '网络异常，请稍后重试'
    handleAuthExpired(errorCode)
    return Promise.reject(rejectWithCode(message, errorCode))
  }
)

/**
 * 业务请求客户端：
 * - 默认响应拦截器已解包为 ApiResult<T>（code/msg/data/errorCode）
 * - rawResponse: true 时原样返回 AxiosResponse（SSE/流式场景）
 */
export interface HttpClient {
  get<T = unknown>(url: string, config: AxiosRequestConfig & { rawResponse: true }): Promise<AxiosResponse<ReadableStream<Uint8Array>>>
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  post(url: string, data: unknown, config: AxiosRequestConfig & { rawResponse: true }): Promise<AxiosResponse<ReadableStream<Uint8Array>>>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
}

const request = instance as unknown as HttpClient

export { TOKEN_KEY, getAuthToken, setAuthToken }
export default request
