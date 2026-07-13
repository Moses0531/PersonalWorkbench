import { fetchUserMenusApi } from '@/apis/system/user/AuthApi'
import { getCurrentUserProfileApi } from '@/apis/system/user/UserProfileApi'
import { buildRoutes, resetDynamicRoutes } from '@/router/dynamicRoutes'
import { useUserStore } from '@/stores/userStore'

/** 从后端重新拉取用户、菜单并重建动态路由 */
export async function syncAppMenu(router) {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) return false

  try {
    const [menuRes, profileRes] = await Promise.all([
      fetchUserMenusApi(),
      getCurrentUserProfileApi(),
    ])
    const rawMenuList = menuRes?.data || []
    userStore.setUserLoginInfo({ menuList: rawMenuList })
    const profile = profileRes?.data
    if (profile) {
      userStore.setAuth(userStore.token, {
        userId: profile.userId,
        account: profile.account,
        username: profile.username,
        avatar: profile.avatar,
        roleId: profile.roleId,
      })
    }
    resetDynamicRoutes(router)
    return buildRoutes(router)
  } catch {
    return false
  }
}
