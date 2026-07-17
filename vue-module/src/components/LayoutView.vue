<script setup>
/** 登录后主布局：悬浮 Dock + 内容区 router-view */
import NavigationBarView from './NavigationBarView.vue'
</script>

<template>
  <!-- 导航壳：悬浮胶囊 Dock + 命令顶栏 + 默认插槽为页面内容 -->
  <NavigationBarView>
    <router-view />
  </NavigationBarView>
</template>
