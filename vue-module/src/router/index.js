import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes } from './routers'
import { useUserStore } from '@/stores/userStore'
import { buildRoutes, resetDynamicRoutes, MAIN_LAYOUT_NAME } from './dynamicRoutes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
})

function ensureDynamicRoutes() {
  const userStore = useUserStore()
  if (userStore.routesBuilt) return true
  if (!userStore.isLoggedIn) return false

  const restored = userStore.restoreMenuFromStorage()
  if (!restored) return false

  return buildRoutes(router)
}

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.path === '/' && !userStore.isLoggedIn) {
    return '/auth'
  }

  if (to.meta.guest) {
    if (userStore.isLoggedIn && to.name === 'auth') {
      const ready = ensureDynamicRoutes()
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
    const ready = ensureDynamicRoutes()
    if (!ready) {
      userStore.clearAuth()
      return { path: '/auth', query: { redirect: to.fullPath } }
    }
    if (!wasBuilt && userStore.routesBuilt) {
      return { ...to, replace: true }
    }
  }

  return true
})

export { buildRoutes, resetDynamicRoutes, MAIN_LAYOUT_NAME, ensureDynamicRoutes }
export default router
