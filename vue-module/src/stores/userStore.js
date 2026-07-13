import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/utils/request'
import { buildMenuState } from '@/utils/menu'

const USER_KEY = 'rbac_user'
const MENU_KEY = 'rbac_menu_v2'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function readStoredMenuList() {
  try {
    const raw = localStorage.getItem(MENU_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
    menuList: [],
    menuTree: [],
    menuRouterList: [],
    menuParentIdListMap: new Map(),
    functionList: [],
    routesBuilt: false,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    getMenuList: (state) => state.menuList,
    getMenuTree: (state) => state.menuTree,
    getMenuRouterList: (state) => state.menuRouterList,
    getMenuParentIdListMap: (state) => state.menuParentIdListMap,
    getFunctionList: (state) => state.functionList,
  },

  actions: {
    setAuth(token, user) {
      this.token = token || ''
      this.user = user || null
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
    },

    clearAuth() {
      this.setAuth('', null)
      this.clearMenu()
      this.routesBuilt = false
    },

    clearMenu() {
      this.menuList = []
      this.menuTree = []
      this.menuRouterList = []
      this.menuParentIdListMap = new Map()
      this.functionList = []
      localStorage.removeItem(MENU_KEY)
    },

    applyMenuState(rawMenuList = []) {
      const state = buildMenuState(rawMenuList)
      this.menuList = state.menuList
      this.menuTree = state.menuTree
      this.menuRouterList = state.menuRouterList
      this.menuParentIdListMap = state.menuParentIdListMap
      this.functionList = state.functionList
    },

    setUserLoginInfo(data) {
      const rawMenuList = data?.menuList || []
      localStorage.setItem(MENU_KEY, JSON.stringify(rawMenuList))
      this.applyMenuState(rawMenuList)
    },

    restoreMenuFromStorage() {
      const rawMenuList = readStoredMenuList()
      if (rawMenuList.length) {
        this.applyMenuState(rawMenuList)
      }
      return rawMenuList.length > 0
    },

    setRoutesBuilt(value) {
      this.routesBuilt = value
    },
  },
})
