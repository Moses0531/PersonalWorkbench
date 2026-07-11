import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '@/utils/request'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/auth',
    },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('../views/system/auth/AuthPage.vue'),
      meta: { guest: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/system/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.meta.requiresAuth && !token) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }
  if (to.meta.guest && token && to.name === 'auth') {
    return { path: '/dashboard' }
  }
})

export default router
