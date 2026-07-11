import axios from 'axios'

const TOKEN_KEY = 'rbac_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers[TOKEN_KEY] = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
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

export { TOKEN_KEY }
export default request
