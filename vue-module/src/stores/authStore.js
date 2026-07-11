import { reactive, readonly } from 'vue'
import { TOKEN_KEY } from '@/utils/request'

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: null,
})

function setAuth(token, user) {
  state.token = token || ''
  state.user = user || null
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

function clearAuth() {
  setAuth('', null)
}

function isLoggedIn() {
  return Boolean(state.token)
}

export function useAuthStore() {
  return {
    state: readonly(state),
    setAuth,
    clearAuth,
    isLoggedIn,
  }
}
