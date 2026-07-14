<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
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
  <a-config-provider
    :locale="zhCN"
    :theme="{ token: { colorPrimary: '#0891b2', borderRadius: 8 } }"
  >
    <RouterView />
  </a-config-provider>

</template>
