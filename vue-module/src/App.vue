<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterView } from 'vue-router'
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
  <RouterView />
</template>
