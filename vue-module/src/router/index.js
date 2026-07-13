import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes } from './routers'
import { useUserStore } from '@/stores/userStore'
import { buildRoutes, resetDynamicRoutes, MAIN_LAYOUT_NAME } from './dynamicRoutes'

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
      const first = userStore.getMenuRouterList?.[0]
      return first?.path ? (first.path.startsWith('/') ? first.path : `/${first.path}`) : '/dashboard'
    }
    return true
  }

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
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
      // 勿 spread to：刷新时可能先命中 404，携带 name 会导致二次导航仍进 404
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
