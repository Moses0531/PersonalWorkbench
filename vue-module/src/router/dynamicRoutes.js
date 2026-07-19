import { useUserStore } from '@/stores/userStore'
import { resolveViewLoader } from '@/utils/pageComponents'
import { ERROR_ROUTE_PATH, ERROR_ROUTE_NAME } from './routers'

export const MAIN_LAYOUT_NAME = 'app-layout'
/** 未匹配路径的兜底路由名（内部使用，最终会重定向到 /error） */
export const FALLBACK_ROUTE_NAME = 'fallback'

export function registerFallbackRoute(router) {
  if (router.hasRoute(FALLBACK_ROUTE_NAME)) {
    router.removeRoute(FALLBACK_ROUTE_NAME)
  }
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: FALLBACK_ROUTE_NAME,
    redirect: { path: ERROR_ROUTE_PATH, replace: true },
  })
}

export function buildRoutes(router) {
  const userStore = useUserStore()
  const menuList = userStore.getMenuRouterList || []
  const routerList = []

  for (const menu of menuList) {
    if (!menu.menuId || !menu.path) continue

    const loader = resolveViewLoader(menu)
    if (!loader) {
      console.warn(`[buildRoutes] 未找到组件: menuId=${menu.menuId}, path=${menu.path}`)
      continue
    }

    routerList.push({
      path: toChildPath(menu.path),
      name: String(menu.menuId),
      component: loader,
      meta: {
        id: String(menu.menuId),
        title: menu.menuName,
        icon: menu.icon,
        hideInMenu: !menu.visibleFlag,
        keepAlive: menu.cacheFlag,
        componentName: String(menu.menuId),
        requiresAuth: true,
      },
    })
  }

  if (router.hasRoute(MAIN_LAYOUT_NAME)) {
    router.removeRoute(MAIN_LAYOUT_NAME)
  }

  if (routerList.length > 0) {
    const dashboardChild = routerList.find((item) => item.path === 'dashboard')
    const redirect = dashboardChild?.path || routerList[0].path
    router.addRoute({
      path: '/',
      name: MAIN_LAYOUT_NAME,
      component: () => import('@/components/LayoutView.vue'),
      redirect,
      children: routerList,
    })
    registerFallbackRoute(router)
  }

  userStore.setRoutesBuilt(routerList.length > 0)
  return routerList.length > 0
}

export function resetDynamicRoutes(router) {
  const userStore = useUserStore()
  if (router.hasRoute(MAIN_LAYOUT_NAME)) {
    router.removeRoute(MAIN_LAYOUT_NAME)
  }
  if (router.hasRoute(FALLBACK_ROUTE_NAME)) {
    router.removeRoute(FALLBACK_ROUTE_NAME)
  }
  userStore.setRoutesBuilt(false)
}

function toChildPath(path) {
  return String(path).replace(/^\//, '')
}

/** 登录后默认跳转路径：优先首页，否则第一个已能解析组件的菜单 */
export function resolveDefaultPath(fallback = '/dashboard') {
  const userStore = useUserStore()
  const menus = userStore.getMenuRouterList || []
  const dashboard = menus.find((item) => normalizePath(item.path) === '/dashboard')
  if (dashboard && resolveViewLoader(dashboard)) {
    return '/dashboard'
  }
  for (const item of menus) {
    if (!item?.path || !resolveViewLoader(item)) continue
    return normalizePath(item.path)
  }
  return fallback
}

function normalizePath(path) {
  const raw = String(path || '').trim()
  if (!raw) return ''
  return raw.startsWith('/') ? raw : `/${raw}`
}

export { ERROR_ROUTE_PATH, ERROR_ROUTE_NAME }
