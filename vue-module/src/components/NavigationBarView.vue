<template>
  <div class="layout">
    <!-- 左侧图标轨道 -->
    <nav class="rail" role="navigation" aria-label="主导航">
      <div class="rail-brand" title="ChassisElevate">
        <BrandMarkView :size="24" />
        <span class="rail-version">v1.0.0</span>
      </div>

      <div class="rail-icons">
        <template v-for="(item, index) in railItems" :key="item.id">
          <div
            v-if="index > 0 && isRailTypeBoundary(railItems[index - 1], item)"
            class="rail-type-divider"
            aria-hidden="true"
          />
          <button
            class="rail-btn"
            :class="{
              active: isRailActive(item),
              'is-directory': isDirectoryRailItem(item),
              'is-menu': isMenuRailItem(item),
              'is-flyout-open': flyoutId === item.id
            }"
            :title="railItemTitle(item)"
            :aria-expanded="hasFlyoutChildren(item) ? flyoutId === item.id : undefined"
            @click="handleRailClick(item, $event)"
          >
            <span class="rail-icon-wrap">
              <component :is="item.icon" class="rail-icon" />
              <span
                v-if="hasFlyoutChildren(item)"
                class="rail-expand-badge"
                aria-hidden="true"
              >
                <RightOutlined />
              </span>
            </span>
            <span class="rail-label">{{ item.label }}</span>
          </button>
        </template>
      </div>

      <div class="rail-bottom">
        <button class="rail-btn" @click="handleLogout" title="退出登录">
          <LogoutOutlined class="rail-icon" />
        </button>
        <div class="rail-avatar" @click="goToProfile" :title="displayName">
          <img :src="userAvatar" class="rail-avatar-img" alt="" @error="onAvatarError" />
        </div>
      </div>
    </nav>

    <!-- 子菜单弹出面板 -->
    <Transition name="flyout">
      <div
        v-if="flyoutId && flyoutChildren.length"
        ref="flyoutRef"
        class="flyout-panel"
        :style="{ top: flyoutTop + 'px' }"
      >
        <div class="flyout-title">{{ flyoutItem?.label }}</div>
        <button
          v-for="child in flyoutChildren"
          :key="child.menuId"
          class="flyout-child"
          :class="{ active: isFlyoutChildActive(child) }"
          @click="selectFlyoutChild(child)"
        >
          <span class="flyout-child-icon" aria-hidden="true">
            <FileOutlined />
          </span>
          <span class="flyout-child-name">{{ child.menuName }}</span>
        </button>
      </div>
    </Transition>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="search-bar">
        <div class="search-bar-left">
          <span class="search-brand">ChassisElevate</span>
          <span v-if="currentPageTitle" class="search-page">{{ currentPageTitle }}</span>
        </div>

        <button class="search-pill" @click="openCmd">
          <SearchOutlined />
          <span class="search-placeholder">搜索菜单...</span>
          <kbd class="search-kbd">Ctrl K</kbd>
        </button>

        <div class="search-right">
          <button class="icon-btn" title="通知">
            <BellOutlined />
          </button>
        </div>
      </header>

      <main class="main-content">
        <div ref="mainScrollRef" class="main-scroll">
          <slot />
        </div>
      </main>
    </div>

    <!-- 命令面板 (Ctrl+K) -->
    <Teleport to="body">
      <div v-if="cmdOpen" class="cmd-overlay" @click.self="closeCmd">
        <div class="cmd-palette" role="dialog" aria-label="命令面板">
          <div class="cmd-input-wrap">
            <SearchOutlined />
            <input
              ref="cmdInputRef"
              v-model="cmdQuery"
              class="cmd-input"
              placeholder="输入菜单名称搜索..."
              @keydown.down.prevent="moveCmd(1)"
              @keydown.up.prevent="moveCmd(-1)"
              @keydown.enter.prevent="selectCmd"
              @keydown.esc.prevent="closeCmd"
            />
            <kbd class="cmd-esc">ESC</kbd>
          </div>
          <div v-if="cmdResults.length" class="cmd-list">
            <button
              v-for="(item, i) in cmdResults"
              :key="item.menuId"
              class="cmd-item"
              :class="{ selected: i === cmdIndex }"
              @click="goToMenu(item)"
              @mouseenter="cmdIndex = i"
            >
              <component :is="getMenuIcon(item.menuName)" class="cmd-item-icon" />
              <span class="cmd-item-label">{{ item.menuName }}</span>
              <span class="cmd-item-path">{{ item.path }}</span>
            </button>
          </div>
          <div v-else-if="cmdQuery" class="cmd-empty">没有找到匹配的菜单</div>
          <div v-else class="cmd-hint">输入关键词搜索所有菜单，按 Enter 跳转</div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
/** 主布局导航：左侧轨道菜单、子菜单飞层、Ctrl+K 命令面板 */
import { computed, onMounted, onBeforeUnmount, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter, isNavigationFailure, NavigationFailureType } from 'vue-router'
import BrandMarkView from './BrandMarkView.vue'
import {
  RightOutlined,
  LogoutOutlined,
  FileOutlined,
  SearchOutlined,
  BellOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/userStore'
import {
  MENU_TYPE,
  isDirectoryType,
  isFunctionType,
  isMenuType,
} from '@/utils/menu'
import { getMenuIcon } from '@/utils/menuIcons'
import { resetDynamicRoutes } from '@/router/dynamicRoutes'
import router from '@/router'

const route = useRoute()
const vueRouter = useRouter()
const userStore = useUserStore()

const flyoutId = ref(null)
const flyoutTop = ref(0)
const flyoutRef = ref(null)
const mainScrollRef = ref(null)

const menuTree = computed(() => userStore.getMenuTree)
const menuList = computed(() => userStore.getMenuList)
const activePath = computed(() => route.path)

const displayName = computed(
  () => userStore.user?.username || userStore.user?.account || '用户',
)

const DEFAULT_AVATAR = 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png'
const avatarLoadFailed = ref(false)

const currentPageTitle = computed(() => {
  const path = activePath.value
  for (const menu of menuList.value) {
    if (!isMenuType(menu)) continue
    if (menu.path === path) return menu.menuName || ''
  }
  return route.meta?.title || ''
})

const userAvatar = computed(() => {
  if (avatarLoadFailed.value) return DEFAULT_AVATAR
  const avatar = userStore.user?.avatar || DEFAULT_AVATAR
  if (/^https?:\/\//.test(avatar)) return avatar
  const base = (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, '')
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`
  return `${base}${path}`
})

watch(() => userStore.user?.avatar, () => { avatarLoadFailed.value = false })

function onAvatarError(e) {
  avatarLoadFailed.value = true
  e.target.src = DEFAULT_AVATAR
}

function filterVisibleChildren(menu) {
  if (!menu.children?.length) return []
  return menu.children.filter((child) => !isFunctionType(child) && canAccessMenu(child))
}

function canAccessMenu(menu) {
  if (!menu.visibleFlag || menu.disabledFlag) return false
  if (isFunctionType(menu)) return false
  if (isDirectoryType(menu)) {
    return menuList.value.some((item) => {
      if (!item.visibleFlag || item.disabledFlag) return false
      if (Number(item.parentId) !== Number(menu.menuId)) return false
      if (isFunctionType(item)) return false
      if (isDirectoryType(item)) return canAccessMenu(item)
      return true
    })
  }
  return true
}

function collectNavigableChildren(menu) {
  const items = []
  function walk(nodes) {
    for (const node of nodes) {
      if (!canAccessMenu(node)) continue
      if (isMenuType(node)) {
        items.push(node)
      } else if (isDirectoryType(node)) {
        walk(filterVisibleChildren(node))
      }
    }
  }
  walk(filterVisibleChildren(menu))
  return items
}

const railItems = computed(() =>
  menuTree.value
    .filter((m) => canAccessMenu(m))
    .map((m) => ({
      id: m.menuId,
      label: m.menuName || '',
      type: m.menuType,
      children: collectNavigableChildren(m),
      menu: m,
      icon: getMenuIcon(m.menuName),
    })),
)

function isDirectoryRailItem(item) {
  return item.type === MENU_TYPE.DIRECTORY
}

function isMenuRailItem(item) {
  return item.type === MENU_TYPE.MENU
}

function hasFlyoutChildren(item) {
  return isDirectoryRailItem(item) && item.children.length > 1
}

function isRailTypeBoundary(prev, curr) {
  return isDirectoryRailItem(prev) !== isDirectoryRailItem(curr)
}

function railItemTitle(item) {
  if (isDirectoryRailItem(item) && item.children.length > 1) {
    return `${item.label} · ${item.children.length}`
  }
  return item.label
}

function resolveRoutePath(menu) {
  return menu?.path || ''
}

function isRailActive(item) {
  const path = activePath.value
  if (isMenuRailItem(item)) {
    return resolveRoutePath(item.menu) === path
  }
  return item.children.some((child) => resolveRoutePath(child) === path)
}

function handleRailClick(item, evt) {
  if (isMenuRailItem(item)) {
    handleMenuSelect(item.menu)
    closeFlyout()
    return
  }
  if (!item.children.length) return
  if (item.children.length === 1) {
    handleMenuSelect(item.children[0])
    closeFlyout()
    return
  }
  if (flyoutId.value === item.id) {
    closeFlyout()
  } else {
    const btn = evt?.currentTarget
    if (btn) {
      flyoutTop.value = btn.getBoundingClientRect().top
    }
    flyoutId.value = item.id
  }
}

const flyoutItem = computed(() =>
  flyoutId.value ? railItems.value.find((r) => r.id === flyoutId.value) : null,
)
const flyoutChildren = computed(() => flyoutItem.value?.children || [])

function closeFlyout() {
  flyoutId.value = null
}

function isFlyoutChildActive(child) {
  return resolveRoutePath(child) === activePath.value
}

function selectFlyoutChild(child) {
  handleMenuSelect(child)
  closeFlyout()
}

const cmdOpen = ref(false)
const cmdQuery = ref('')
const cmdIndex = ref(0)
const cmdInputRef = ref(null)

const flatMenuList = computed(() => {
  const result = []
  function walk(list) {
    for (const m of list) {
      if (canAccessMenu(m) && isMenuType(m)) result.push(m)
      if (m.children?.length) walk(m.children)
    }
  }
  walk(menuTree.value)
  return result
})

const cmdResults = computed(() => {
  if (!cmdQuery.value) return []
  const q = cmdQuery.value.toLowerCase()
  return flatMenuList.value
    .filter((m) => (m.menuName || '').toLowerCase().includes(q))
    .slice(0, 12)
})

function openCmd() {
  cmdOpen.value = true
  cmdQuery.value = ''
  cmdIndex.value = 0
  nextTick(() => cmdInputRef.value?.focus())
}

function closeCmd() {
  cmdOpen.value = false
  cmdQuery.value = ''
}

function moveCmd(delta) {
  const len = cmdResults.value.length
  if (!len) return
  cmdIndex.value = (cmdIndex.value + delta + len) % len
}

function selectCmd() {
  const item = cmdResults.value[cmdIndex.value]
  if (item) goToMenu(item)
}

function goToMenu(item) {
  handleMenuSelect(item)
  closeCmd()
}

function handleGlobalKey(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    cmdOpen.value ? closeCmd() : openCmd()
  }
  if (e.key === 'Escape') {
    if (cmdOpen.value) closeCmd()
    else if (flyoutId.value) closeFlyout()
  }
}

watch(cmdQuery, () => {
  cmdIndex.value = 0
})

watch(activePath, () => {
  closeFlyout()
})

function goToProfile() {
  const profile = menuList.value.find((m) => m.path === '/profile')
  if (profile) handleMenuSelect(profile)
}

function handleMenuSelect(menu) {
  const name = String(menu.menuId)
  const targetPath = resolveRoutePath(menu)
  const label = menu.menuName || '该菜单'

  function pushPath() {
    if (!targetPath) return
    vueRouter.push(targetPath).catch((failure) => {
      if (isNavigationFailure(failure, NavigationFailureType.duplicated)) return
      console.warn(`无法打开「${label}」`)
    })
  }

  if (isMenuType(menu) && vueRouter.hasRoute(name)) {
    vueRouter.push({ name }).catch((failure) => {
      if (isNavigationFailure(failure, NavigationFailureType.duplicated)) return
      pushPath()
    })
    return
  }
  pushPath()
}

async function handleLogout() {
  userStore.clearAuth()
  resetDynamicRoutes(router)
  await vueRouter.replace('/auth')
}

watch(
  () => route.fullPath,
  async () => {
    await nextTick()
    const el = mainScrollRef.value
    if (el && typeof el.scrollTo === 'function') {
      el.scrollTo({ top: 0, behavior: 'auto' })
      return
    }
    if (el) el.scrollTop = 0
  },
)

function handleDocClick(e) {
  if (!flyoutId.value) return
  const flyout = flyoutRef.value
  const rail = document.querySelector('.rail')
  if (flyout?.contains(e.target)) return
  if (rail?.contains(e.target)) return
  closeFlyout()
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKey)
  document.addEventListener('mousedown', handleDocClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKey)
  document.removeEventListener('mousedown', handleDocClick)
})
</script>

<style scoped>
.layout {
  height: 100dvh;
  display: flex;
  overflow: hidden;
  font-family: var(--font-family-sans);
  background: var(--color-bg);
}

/* ═══════════════════════════════
 *  图标轨道
 * ═══════════════════════════════ */
.rail {
  width: var(--rail-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--color-zone-sidebar);
  border-right: 1px solid var(--color-zone-sidebar-border);
  padding: 10px 0 12px;
  z-index: var(--z-sticky);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.55);
}

.rail-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 4px 0 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--color-zone-sidebar-border);
  width: calc(100% - 16px);
  color: var(--color-accent);
}

.rail-version {
  font-family: var(--font-family-mono);
  font-size: 9px;
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  letter-spacing: 0.06em;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: rgba(8, 145, 178, 0.08);
  border: 1px solid rgba(8, 145, 178, 0.14);
}

.rail-icons {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  width: 100%;
  padding: 0 8px;
}

.rail-icons::-webkit-scrollbar {
  display: none;
}

.rail-type-divider {
  width: calc(100% - 12px);
  height: 1px;
  margin: 6px 0 4px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(8, 145, 178, 0.22) 18%,
    rgba(8, 145, 178, 0.22) 82%,
    transparent 100%
  );
  flex-shrink: 0;
}

.rail-btn {
  width: 60px;
  min-height: 52px;
  border-radius: var(--radius-control, 10px);
  border: none;
  background: transparent;
  color: var(--color-text-dim);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 6px 4px;
  cursor: pointer;
  transition:
    background var(--transition-normal),
    color var(--transition-normal),
    box-shadow var(--transition-normal),
    transform var(--transition-fast);
  position: relative;
  flex-shrink: 0;
}

.rail-icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

.rail-icon {
  display: block;
  font-size: 20px;
}

.rail-expand-badge :deep(.anticon) {
  font-size: 8px;
}

.rail-btn.is-directory .rail-icon-wrap {
  border: 1px dashed rgba(8, 145, 178, 0.34);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.42);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.rail-btn.is-menu .rail-icon-wrap {
  border-radius: 8px;
}

.rail-expand-badge {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--color-zone-sidebar-elevated);
  border: 1px solid rgba(8, 145, 178, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent);
  box-shadow: 0 1px 2px rgba(8, 116, 146, 0.12);
  transition: transform var(--transition-normal), background var(--transition-fast);
}

.rail-btn.is-directory.is-flyout-open .rail-expand-badge {
  transform: rotate(90deg);
  background: var(--color-accent-soft);
  border-color: rgba(8, 145, 178, 0.42);
}

.rail-btn:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.rail-btn.is-directory:hover .rail-icon-wrap {
  border-color: rgba(8, 145, 178, 0.52);
  background: rgba(255, 255, 255, 0.58);
}

.rail-btn.is-menu:hover .rail-icon-wrap {
  background: rgba(255, 255, 255, 0.35);
}

.rail-btn:active {
  transform: scale(0.97);
}

.rail-btn.active {
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
}

.rail-btn.is-directory.active .rail-icon-wrap {
  border-style: solid;
  border-color: rgba(8, 145, 178, 0.48);
  background: rgba(255, 255, 255, 0.72);
}

.rail-btn.is-menu.active .rail-icon-wrap {
  background: rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 0 0 1px rgba(8, 145, 178, 0.16);
}

.rail-btn.is-menu.active::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: var(--color-accent);
  border-radius: 0 0 3px 3px;
}

.rail-btn.is-directory.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 22px;
  background: var(--color-accent);
  border-radius: 0 3px 3px 0;
}

.rail-btn.is-directory.is-flyout-open {
  background: rgba(34, 184, 207, 0.16);
  box-shadow: inset 0 0 0 1px rgba(8, 145, 178, 0.14);
}

.rail-btn:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.rail-label {
  font-size: 10px;
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 58px;
  text-align: center;
  font-weight: var(--font-medium);
}

.rail-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--color-zone-sidebar-border);
  margin-top: 8px;
  width: 100%;
}

.rail-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  border: 2px solid var(--color-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent-soft);
  transition: border-color var(--transition-fast);
}

.rail-avatar:hover {
  border-color: var(--color-accent);
}

.rail-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ═══════════════════════════════
 *  子菜单弹出面板
 * ═══════════════════════════════ */
.flyout-panel {
  position: fixed;
  left: var(--rail-width);
  z-index: var(--z-dropdown);
  min-width: 180px;
  max-width: 240px;
  max-height: 70vh;
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  scrollbar-width: thin;
}

.flyout-title {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  padding: 6px 12px 8px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 4px;
  letter-spacing: 0.02em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flyout-child {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text-body);
  font-size: var(--text-sm);
  font-family: inherit;
  text-align: left;
  width: 100%;
  transition: all var(--transition-fast);
}

.flyout-child:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.flyout-child.active {
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
  font-weight: var(--font-medium);
}

.flyout-child-icon {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(8, 145, 178, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-dim);
  flex-shrink: 0;
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    border-color var(--transition-fast);
}

.flyout-child:hover .flyout-child-icon,
.flyout-child.active .flyout-child-icon {
  background: rgba(255, 255, 255, 0.95);
  color: var(--color-accent);
  border-color: rgba(8, 145, 178, 0.24);
}

.flyout-child-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flyout-enter-active {
  transition: all 0.18s ease-out;
}

.flyout-leave-active {
  transition: all 0.12s ease-in;
}

.flyout-enter-from,
.flyout-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* ═══════════════════════════════
 *  主区域
 * ═══════════════════════════════ */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: 8px 20px;
  flex-shrink: 0;
  background: var(--color-zone-content);
  border-bottom: 1px solid var(--color-border-light);
}

.search-bar-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  flex-shrink: 0;
}

.search-brand {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  white-space: nowrap;
}

.search-page {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  padding-left: var(--space-2);
  border-left: 1px solid var(--color-border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.search-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 14px;
  border-radius: var(--radius-control, 10px);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  transition: all var(--transition-normal);
  flex: 1;
  max-width: 360px;
  color: var(--color-text-dim);
}

.search-pill:hover {
  border-color: var(--color-accent-glow);
  box-shadow: 0 0 0 3px var(--color-accent-muted);
}

.search-placeholder {
  font-size: var(--text-sm);
  color: var(--color-text-dim);
  flex: 1;
  text-align: left;
}

.search-kbd {
  font-size: 10px;
  padding: 2px 6px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text-dim);
  font-family: var(--font-family-mono);
  background: var(--color-bg);
}

.search-right {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-left: auto;
  flex-shrink: 0;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-control, 10px);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.icon-btn:hover {
  color: var(--color-accent);
  border-color: var(--color-accent-glow);
  background: var(--color-accent-soft);
}

.icon-btn:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  background: var(--color-zone-content);
}

.main-content::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background: var(--color-zone-content-glow);
}

.main-scroll {
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  position: relative;
  z-index: 1;
}

.main-scroll::-webkit-scrollbar {
  width: 5px;
}

.main-scroll::-webkit-scrollbar-thumb {
  background: rgba(34, 184, 207, 0.2);
  border-radius: 3px;
}

.main-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(34, 184, 207, 0.25);
}

.main-scroll::-webkit-scrollbar-track {
  background: transparent;
}

/* ═══════════════════════════════
 *  命令面板
 * ═══════════════════════════════ */
.cmd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(12, 42, 66, 0.28);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: var(--z-modal);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15vh;
}

.cmd-palette {
  width: 520px;
  max-height: 420px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: cmdIn 0.15s ease-out;
}

@keyframes cmdIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.cmd-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-dim);
}

.cmd-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: var(--text-md);
  color: var(--color-text-primary);
  background: transparent;
  font-family: inherit;
}

.cmd-input::placeholder {
  color: var(--color-text-muted);
}

.cmd-esc {
  font-size: 10px;
  padding: 2px 6px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text-dim);
  font-family: var(--font-family-mono);
}

.cmd-list {
  overflow-y: auto;
  padding: 6px;
}

.cmd-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
  text-align: left;
  font-family: inherit;
  color: var(--color-text-primary);
}

.cmd-item:hover,
.cmd-item.selected {
  background: var(--color-accent-soft);
}

.cmd-item-icon {
  font-size: 16px;
  color: var(--color-text-dim);
  flex-shrink: 0;
}

.cmd-item-label {
  font-size: var(--text-md);
  font-weight: var(--font-medium);
  flex: 1;
}

.cmd-item-path {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: var(--font-family-mono);
}

.cmd-empty,
.cmd-hint {
  padding: 24px;
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-dim);
}

@media (max-width: 768px) {
  .search-bar-left {
    display: none;
  }

  .search-pill {
    max-width: none;
  }

  .search-kbd {
    display: none;
  }

  .cmd-palette {
    width: calc(100vw - 32px);
  }

  .rail-label {
    display: none;
  }

  .rail-btn {
    min-height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flyout-enter-active,
  .flyout-leave-active,
  .cmd-palette {
    animation: none;
    transition: none;
  }
}
</style>
