<template>
  <div class="app-layout">
    <aside class="app-layout__aside">
      <div class="app-layout__brand">Spine Extension</div>
      <SideMenu />
    </aside>
    <section class="app-layout__main">
      <header class="app-layout__header">
        <span class="app-layout__welcome">欢迎，{{ displayName }}</span>
        <el-button type="danger" link @click="handleLogout">退出登录</el-button>
      </header>
      <main class="app-layout__content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { resetDynamicRoutes } from '@/router/index'
import { useUserStore } from '@/stores/userStore'
import SideMenu from './components/side-menu/index.vue'

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(
  () => userStore.user?.username || userStore.user?.account || '用户',
)

function handleLogout() {
  userStore.clearAuth()
  resetDynamicRoutes()
  router.replace('/auth')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.app-layout__aside {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #1d2b3a;
  color: #fff;
}

.app-layout__brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.app-layout__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-layout__header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.app-layout__welcome {
  color: #303133;
  font-size: 14px;
}

.app-layout__content {
  flex: 1;
  padding: 24px;
  overflow: auto;
}
</style>
