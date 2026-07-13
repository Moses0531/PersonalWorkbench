<script setup>
/** 登录后主布局：左侧导航轨道 + 内容区 router-view */
import NavigationBarView from './NavigationBarView.vue'
</script>

<template>
  <!-- 导航壳：左侧轨道 + 顶栏 + 默认插槽为页面内容 -->
  <NavigationBarView>
    <router-view />
  </NavigationBarView>
</template>
