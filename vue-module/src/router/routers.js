/** 不受权限控制的静态路由（未匹配路径在动态路由注册后统一重定向至 /error） */
export const ERROR_ROUTE_PATH = '/error'
export const ERROR_ROUTE_NAME = 'error'

export const constantRoutes = [
  {
    path: '/auth',
    name: 'auth',
    component: () => import('@/views/system/user/AuthPage.vue'),
    meta: { guest: true },
  },
  {
    path: ERROR_ROUTE_PATH,
    name: ERROR_ROUTE_NAME,
    component: () => import('@/views/common/ErrorPage.vue'),
    meta: { requiresAuth: true, title: '页面无法访问' },
  },
  // 兼容旧地址，统一落到同一错误页
  { path: '/403', redirect: ERROR_ROUTE_PATH },
  { path: '/404', redirect: ERROR_ROUTE_PATH },
]
