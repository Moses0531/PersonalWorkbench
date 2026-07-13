import { useUserStore } from '@/stores/userStore'
import { resolveViewLoader } from '@/utils/pageComponents'

export const MAIN_LAYOUT_NAME = 'app-layout'
export const NOT_FOUND_ROUTE_NAME = '404'

export function registerNotFoundRoute(router) {
  if (router.hasRoute(NOT_FOUND_ROUTE_NAME)) {
    router.removeRoute(NOT_FOUND_ROUTE_NAME)
  }
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: NOT_FOUND_ROUTE_NAME,
    component: () => import('@/views/common/NotFoundPage.vue'),
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
    const redirect = routerList[0].path
    router.addRoute({
      path: '/',
      name: MAIN_LAYOUT_NAME,
      component: () => import('@/components/LayoutView.vue'),
      redirect,
      children: routerList,
    })
    registerNotFoundRoute(router)
  }

  userStore.setRoutesBuilt(routerList.length > 0)
  return routerList.length > 0
}

export function resetDynamicRoutes(router) {
  const userStore = useUserStore()
  if (router.hasRoute(MAIN_LAYOUT_NAME)) {
    router.removeRoute(MAIN_LAYOUT_NAME)
  }
  if (router.hasRoute(NOT_FOUND_ROUTE_NAME)) {
    router.removeRoute(NOT_FOUND_ROUTE_NAME)
  }
  userStore.setRoutesBuilt(false)
}

function toChildPath(path) {
  return String(path).replace(/^\//, '')
}

/** 登录后默认跳转路径：优先 redirect，否则第一个可路由菜单 */
export function resolveDefaultPath(fallback = '/dashboard') {
  const userStore = useUserStore()
  const first = userStore.getMenuRouterList?.[0]
  if (first?.path) {
    return first.path.startsWith('/') ? first.path : `/${first.path}`
  }
  return fallback
}
