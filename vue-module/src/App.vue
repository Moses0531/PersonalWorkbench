<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import router from '@/router'
import { syncAppMenu } from '@/utils/syncAppMenu'

async function handlePermissionsUpdated() {
  await syncAppMenu(router)
}

onMounted(() => {
  window.addEventListener('permissions-updated', handlePermissionsUpdated)
})

onBeforeUnmount(() => {
  window.removeEventListener('permissions-updated', handlePermissionsUpdated)
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <RouterView />
  </el-config-provider>
</template>
