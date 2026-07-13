import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/utils/request'
import { buildMenuState } from '@/utils/menu'
import { fetchUserMenusApi } from '@/apis/system/user/AuthApi'
import { getCurrentUserProfileApi } from '@/apis/system/user/UserProfileApi'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
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
      this.applyMenuState(rawMenuList)
      this.routesBuilt = false
    },

    /** 登录态下从后端实时刷新用户与菜单，不读本地业务缓存 */
    async refreshSessionFromServer() {
      if (!this.isLoggedIn) return false
      try {
        const [menuRes, profileRes] = await Promise.all([
          fetchUserMenusApi(),
          getCurrentUserProfileApi(),
        ])
        const rawMenuList = menuRes?.data || []
        this.applyMenuState(rawMenuList)
        const profile = profileRes?.data
        if (profile) {
          this.user = {
            userId: profile.userId,
            account: profile.account,
            username: profile.username,
            avatar: profile.avatar,
            roleId: profile.roleId,
          }
        }
        this.routesBuilt = false
        return true
      } catch {
        return false
      }
    },

    setRoutesBuilt(value) {
      this.routesBuilt = value
    },
  },
})
