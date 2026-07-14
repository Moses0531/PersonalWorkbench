<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import router from '@/router'
import { syncAppMenu } from '@/utils/syncAppMenu'

const antTheme = {
  token: {
    colorPrimary: '#0891b2',
    colorInfo: '#0891b2',
    colorSuccess: '#4aba6a',
    colorWarning: '#d69e2e',
    colorError: '#e05545',
    borderRadius: 8,
    borderRadiusLG: 12,
    borderRadiusSM: 6,
    fontFamily:
      "'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBgLayout: '#f0f7fb',
    colorBorder: 'rgba(34, 184, 207, 0.22)',
    colorBorderSecondary: 'rgba(34, 184, 207, 0.12)',
    controlHeight: 36,
    controlHeightLG: 44,
    controlHeightSM: 28,
    colorText: '#3d6478',
    colorTextHeading: '#0c2a42',
    colorTextSecondary: '#6a8fa3',
    colorTextTertiary: '#8eb4c9',
    colorLink: '#0891b2',
    colorLinkHover: '#0e7490',
    wireframe: false
  }
}

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
  <a-config-provider :locale="zhCN" :theme="antTheme">
    <RouterView />
  </a-config-provider>
</template>
