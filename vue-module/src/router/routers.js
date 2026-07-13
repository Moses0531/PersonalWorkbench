/** 不受权限控制的静态路由 */
export const constantRoutes = [
  {
    path: '/auth',
    name: 'auth',
    component: () => import('@/views/system/auth/AuthPage.vue'),
    meta: { guest: true },
  },
  {
    path: '/403',
    name: '403',
    component: () => import('@/views/common/ForbiddenPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: '404',
    component: () => import('@/views/common/NotFoundPage.vue'),
  },
]
