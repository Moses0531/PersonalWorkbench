import { reactive, readonly } from 'vue'
import { TOKEN_KEY } from '@/utils/request'

const USER_KEY = 'rbac_user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readStoredUser(),
})

function setAuth(token, user) {
  state.token = token || ''
  state.user = user || null
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
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
