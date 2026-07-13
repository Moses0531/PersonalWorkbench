/** 不受权限控制的静态路由（404 在动态路由注册后再追加，避免刷新时误匹配） */
export const constantRoutes = [
  {
    path: '/auth',
    name: 'auth',
    component: () => import('@/views/system/user/AuthPage.vue'),
    meta: { guest: true },
  },
  {
    path: '/403',
    name: '403',
    component: () => import('@/views/common/ForbiddenPage.vue'),
    meta: { requiresAuth: true },
  },
]
