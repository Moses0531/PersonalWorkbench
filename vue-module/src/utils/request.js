import axios from 'axios'

const TOKEN_KEY = 'rbac_token'

/** 按标签页隔离登录态，避免多标签共用同一账号 */
function getAuthToken() {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

function setAuthToken(token) {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

// 清理旧版 localStorage 共享 token，防止与标签页隔离逻辑冲突
localStorage.removeItem(TOKEN_KEY)

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers[TOKEN_KEY] = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // SSE / 流式等场景需要完整响应（含 status、ReadableStream）
    if (response.config.rawResponse) {
      return response
    }
    const result = response.data
    if (result?.code !== 1) {
      return Promise.reject(new Error(result?.msg || '请求失败'))
    }
    return result
  },
  (error) => {
    const message =
      error.response?.data?.msg ||
      error.message ||
      '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export { TOKEN_KEY, getAuthToken, setAuthToken }
export default request
