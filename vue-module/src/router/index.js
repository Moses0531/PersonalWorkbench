import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes } from './routers'
import { useUserStore } from '@/stores/userStore'
import { buildRoutes, resetDynamicRoutes, MAIN_LAYOUT_NAME, FALLBACK_ROUTE_NAME, ERROR_ROUTE_NAME, resolveDefaultPath } from './dynamicRoutes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
})

let menuRefreshPromise = null

async function ensureDynamicRoutes() {
  const userStore = useUserStore()
  if (userStore.routesBuilt) return true
  if (!userStore.isLoggedIn) return false

  if (!menuRefreshPromise) {
    menuRefreshPromise = userStore.refreshSessionFromServer().finally(() => {
      menuRefreshPromise = null
    })
  }
  const hasMenu = await menuRefreshPromise
  if (!hasMenu) return false

  return buildRoutes(router)
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (to.path === '/' && !userStore.isLoggedIn) {
    return '/auth'
  }

  if (to.meta.guest) {
    if (userStore.isLoggedIn && to.name === 'auth') {
      const ready = await ensureDynamicRoutes()
      if (!ready) {
        userStore.clearAuth()
        return true
      }
      // 已登录访问登录页：进首页（或第一个可路由菜单）
      return resolveDefaultPath('/dashboard')
    }
    return true
  }

  // 仅对明确要求登录的路由拦截；未匹配路径 meta 为空，不能当成 requiresAuth
  if (to.matched.some((record) => record.meta.requiresAuth === true) && !userStore.isLoggedIn) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }

  if (userStore.isLoggedIn) {
    const wasBuilt = userStore.routesBuilt
    const ready = await ensureDynamicRoutes()
    if (!ready) {
      userStore.clearAuth()
      return { path: '/auth', query: { redirect: to.fullPath } }
    }
    if (!wasBuilt && userStore.routesBuilt) {
      // 勿 spread to：刷新时可能先命中 fallback，携带 name 会导致二次导航仍进错误页
      if (to.name === FALLBACK_ROUTE_NAME || to.name === ERROR_ROUTE_NAME) {
        return true
      }
      return {
        path: to.fullPath,
        query: to.query,
        hash: to.hash,
        replace: true,
      }
    }
  }

  return true
})

export { buildRoutes, resetDynamicRoutes, MAIN_LAYOUT_NAME, ensureDynamicRoutes }
export default router
