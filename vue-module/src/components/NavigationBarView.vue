<template>
  <div class="layout">
    <!-- 悬浮胶囊 Dock：图标直达，目录展开飞层 -->
    <nav class="dock" role="navigation" aria-label="主导航">
      <button
        type="button"
        class="dock-brand"
        title="Workbench · 打开命令面板"
        aria-label="打开命令面板"
        @click="openCmd"
      >
        <BrandMarkView :size="22" />
      </button>

      <div class="dock-icons">
        <template v-for="(item, index) in railItems" :key="item.id">
          <div
            v-if="index > 0 && isRailTypeBoundary(railItems[index - 1], item)"
            class="dock-divider"
            aria-hidden="true"
          />
          <button
            type="button"
            class="dock-btn"
            :class="{
              active: isRailActive(item),
              'is-directory': isDirectoryRailItem(item),
              'is-menu': isMenuRailItem(item),
              'is-flyout-open': flyoutId === item.id,
            }"
            :title="railItemTitle(item)"
            :aria-label="railItemTitle(item)"
            :aria-expanded="hasFlyoutChildren(item) ? flyoutId === item.id : undefined"
            @click="handleRailClick(item, $event)"
          >
            <span class="dock-icon-wrap">
              <component :is="item.icon" class="dock-icon" />
              <span
                v-if="hasFlyoutChildren(item)"
                class="dock-chevron"
                aria-hidden="true"
              />
            </span>
            <span class="dock-label">{{ item.label }}</span>
          </button>
        </template>
      </div>

      <div class="dock-bottom">
        <button
          type="button"
          class="dock-btn dock-btn--quiet"
          title="退出登录"
          aria-label="退出登录"
          @click="handleLogout"
        >
          <span class="dock-icon-wrap">
            <LogoutOutlined class="dock-icon" />
          </span>
          <span class="dock-label">退出</span>
        </button>
        <button
          type="button"
          class="dock-avatar"
          :title="displayName"
          :aria-label="`个人中心 · ${displayName}`"
          @click="goToProfile"
        >
          <img :src="userAvatar" class="dock-avatar-img" alt="" @error="onAvatarError" />
        </button>
      </div>
    </nav>

    <!-- 子菜单飞层 -->
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
          type="button"
          class="flyout-child"
          :class="{ active: isFlyoutChildActive(child) }"
          @click="selectFlyoutChild(child)"
        >
          <span class="flyout-child-name">{{ child.menuName }}</span>
          <kbd v-if="isFlyoutChildActive(child)" class="flyout-here">当前</kbd>
        </button>
      </div>
    </Transition>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-crumb" v-if="currentPageTitle">
          <span class="topbar-crumb-label">{{ currentPageTitle }}</span>
        </div>
        <div v-else class="topbar-crumb topbar-crumb--empty" aria-hidden="true" />

        <button type="button" class="cmd-trigger" @click="openCmd">
          <SearchOutlined class="cmd-trigger-icon" />
          <span class="cmd-trigger-text">跳转菜单、搜索…</span>
          <kbd class="cmd-trigger-kbd">
            <span class="cmd-trigger-mod">{{ modKey }}</span>K
          </kbd>
        </button>

        <div class="topbar-right">
          <button type="button" class="topbar-icon" title="通知" aria-label="通知">
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

    <!-- 命令面板 -->
    <Teleport to="body">
      <div v-if="cmdOpen" class="cmd-overlay" @click.self="closeCmd">
        <div class="cmd-palette" role="dialog" aria-label="命令面板" aria-modal="true">
          <div class="cmd-input-wrap">
            <SearchOutlined />
            <input
              ref="cmdInputRef"
              v-model="cmdQuery"
              class="cmd-input"
              placeholder="输入菜单名称，Enter 跳转"
              @keydown.down.prevent="moveCmd(1)"
              @keydown.up.prevent="moveCmd(-1)"
              @keydown.enter.prevent="selectCmd"
              @keydown.esc.prevent="closeCmd"
            />
            <kbd class="cmd-esc">ESC</kbd>
          </div>
          <div v-if="!cmdQuery && recentMenus.length" class="cmd-section">
            <div class="cmd-section-label">最近</div>
            <div class="cmd-list">
              <button
                v-for="(item, i) in recentMenus"
                :key="'r-' + item.menuId"
                type="button"
                class="cmd-item"
                :class="{ selected: i === cmdIndex && !cmdResults.length }"
                @click="goToMenu(item)"
                @mouseenter="cmdIndex = i"
              >
                <component :is="getMenuIcon(item.menuName)" class="cmd-item-icon" />
                <span class="cmd-item-label">{{ item.menuName }}</span>
                <span class="cmd-item-path">{{ item.path }}</span>
              </button>
            </div>
          </div>
          <div v-if="cmdResults.length" class="cmd-list">
            <button
              v-for="(item, i) in cmdResults"
              :key="item.menuId"
              type="button"
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
          <div v-else-if="cmdQuery" class="cmd-empty">没有匹配的菜单</div>
          <div v-else-if="!recentMenus.length" class="cmd-hint">
            输入关键词搜索，或点左侧图标直达
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
/** 主布局：悬浮胶囊 Dock + 命令优先顶栏 + Ctrl+K 面板 */
import { computed, onMounted, onBeforeUnmount, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter, isNavigationFailure, NavigationFailureType } from 'vue-router'
import BrandMarkView from './BrandMarkView.vue'
import {
  LogoutOutlined,
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

const RECENT_KEY = 'wb_nav_recent'
const RECENT_MAX = 5

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

const modKey = computed(() =>
  typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl',
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
      const rect = btn.getBoundingClientRect()
      flyoutTop.value = Math.min(
        rect.top,
        window.innerHeight - 280,
      )
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
const recentIds = ref([])

function loadRecent() {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    recentIds.value = raw ? JSON.parse(raw) : []
  } catch {
    recentIds.value = []
  }
}

function pushRecent(menu) {
  if (!menu?.menuId) return
  const id = String(menu.menuId)
  const next = [id, ...recentIds.value.filter((x) => x !== id)].slice(0, RECENT_MAX)
  recentIds.value = next
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(next))
  } catch {
    /* ignore quota */
  }
}

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

const recentMenus = computed(() => {
  const byId = new Map(flatMenuList.value.map((m) => [String(m.menuId), m]))
  return recentIds.value.map((id) => byId.get(id)).filter(Boolean)
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
  const list = cmdQuery.value ? cmdResults.value : recentMenus.value
  const len = list.length
  if (!len) return
  cmdIndex.value = (cmdIndex.value + delta + len) % len
}

function selectCmd() {
  const list = cmdQuery.value ? cmdResults.value : recentMenus.value
  const item = list[cmdIndex.value]
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
  pushRecent(menu)

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
  const dock = document.querySelector('.dock')
  if (flyout?.contains(e.target)) return
  if (dock?.contains(e.target)) return
  closeFlyout()
}

onMounted(() => {
  loadRecent()
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
  position: relative;
}

/* ═══════════════════════════════
 *  悬浮胶囊 Dock
 * ═══════════════════════════════ */
.dock {
  position: fixed;
  left: var(--dock-inset);
  top: var(--dock-inset);
  bottom: var(--dock-inset);
  width: var(--dock-width);
  z-index: var(--z-sticky);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72) 0%, rgba(236, 246, 252, 0.88) 100%);
  border: 1px solid rgba(8, 145, 178, 0.18);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.85) inset,
    0 12px 32px rgba(8, 116, 146, 0.12),
    0 2px 8px rgba(12, 42, 66, 0.06);
  backdrop-filter: blur(var(--blur-lg));
  -webkit-backdrop-filter: blur(var(--blur-lg));
}

.dock-brand {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 14px;
  background: rgba(8, 145, 178, 0.1);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-bottom: 8px;
  flex-shrink: 0;
  transition:
    background var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.dock-brand:hover {
  background: rgba(8, 145, 178, 0.18);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(8, 116, 146, 0.16);
}

.dock-brand:active {
  transform: scale(0.96);
}

.dock-brand:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.dock-icons {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  width: 100%;
  padding: 4px 0;
  min-height: 0;
}

.dock-icons::-webkit-scrollbar {
  display: none;
}

.dock-divider {
  width: 20px;
  height: 1px;
  margin: 4px 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(8, 145, 178, 0.28),
    transparent
  );
  flex-shrink: 0;
}

.dock-btn {
  position: relative;
  width: 60px;
  min-height: 52px;
  padding: 6px 4px;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: var(--color-text-dim);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.dock-icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.dock-icon {
  font-size: 18px;
  display: block;
}

.dock-label {
  font-size: 10px;
  line-height: 1.15;
  font-weight: var(--font-medium);
  text-align: center;
  max-width: 56px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dock-btn:hover {
  background: rgba(8, 145, 178, 0.1);
  color: var(--color-accent);
}

.dock-btn:active {
  transform: scale(0.94);
}

.dock-btn:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.dock-btn.active {
  background: rgba(8, 145, 178, 0.16);
  color: var(--color-accent-deep);
  box-shadow: 0 0 0 1px rgba(8, 145, 178, 0.2);
}

.dock-btn.active::before {
  content: '';
  position: absolute;
  left: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 0 3px 3px 0;
  background: var(--color-accent);
}

.dock-btn.is-flyout-open {
  background: rgba(34, 184, 207, 0.2);
  color: var(--color-accent-deep);
}

.dock-btn--quiet {
  color: var(--color-text-muted);
}

.dock-chevron {
  position: absolute;
  right: -2px;
  bottom: -1px;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 3px 0 3px 4px;
  border-color: transparent transparent transparent var(--color-accent);
  opacity: 0.7;
}

.dock-btn.is-flyout-open .dock-chevron {
  transform: rotate(90deg);
  opacity: 1;
}

.dock-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  margin-top: 4px;
  border-top: 1px solid rgba(8, 145, 178, 0.14);
  width: calc(100% - 16px);
  flex-shrink: 0;
}

.dock-avatar {
  width: 32px;
  height: 32px;
  border-radius: 11px;
  overflow: hidden;
  border: none;
  padding: 0;
  cursor: pointer;
  background: var(--color-accent-soft);
  box-shadow: 0 0 0 2px rgba(8, 145, 178, 0.2);
  transition:
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.dock-avatar:hover {
  box-shadow: 0 0 0 2px var(--color-accent);
  transform: translateY(-1px);
}

.dock-avatar:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.dock-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ═══════════════════════════════
 *  子菜单飞层
 * ═══════════════════════════════ */
.flyout-panel {
  position: fixed;
  left: calc(var(--dock-inset) + var(--dock-width) + 10px);
  z-index: var(--z-dropdown);
  min-width: 168px;
  max-width: 220px;
  max-height: 70vh;
  overflow-y: auto;
  padding: 8px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(8, 145, 178, 0.16);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 16px 40px rgba(8, 116, 146, 0.14);
  backdrop-filter: blur(var(--blur-lg));
  -webkit-backdrop-filter: blur(var(--blur-lg));
  display: flex;
  flex-direction: column;
  gap: 2px;
  scrollbar-width: thin;
}

.flyout-title {
  font-size: 11px;
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  padding: 4px 10px 8px;
  letter-spacing: 0.04em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flyout-child {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  border: none;
  background: transparent;
  border-radius: 10px;
  cursor: pointer;
  color: var(--color-text-body);
  font-size: var(--text-sm);
  font-family: inherit;
  text-align: left;
  width: 100%;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.flyout-child:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
}

.flyout-child.active {
  background: rgba(8, 145, 178, 0.12);
  color: var(--color-accent-deep);
  font-weight: var(--font-medium);
}

.flyout-child-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flyout-here {
  font-size: 9px;
  font-family: var(--font-family-mono);
  padding: 1px 5px;
  border-radius: 4px;
  border: 1px solid rgba(8, 145, 178, 0.28);
  color: var(--color-accent);
  background: rgba(255, 255, 255, 0.7);
}

.flyout-enter-active {
  transition: opacity 0.16s ease-out, transform 0.16s ease-out;
}

.flyout-leave-active {
  transition: opacity 0.1s ease-in, transform 0.1s ease-in;
}

.flyout-enter-from,
.flyout-leave-to {
  opacity: 0;
  transform: translateX(-6px) scale(0.98);
}

/* ═══════════════════════════════
 *  主区域 · 命令优先顶栏
 * ═══════════════════════════════ */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  margin-left: var(--dock-gutter);
}

.topbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(200px, 420px) minmax(0, 1fr);
  align-items: center;
  gap: var(--space-3);
  height: var(--topbar-height);
  padding: 0 20px 0 8px;
  flex-shrink: 0;
  background: transparent;
}

.topbar-crumb {
  min-width: 0;
  justify-self: start;
}

.topbar-crumb-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  letter-spacing: 0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-crumb--empty {
  min-height: 1px;
}

.cmd-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 12px 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(8, 145, 178, 0.16);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.9) inset;
  cursor: pointer;
  color: var(--color-text-dim);
  transition:
    border-color var(--transition-normal),
    box-shadow var(--transition-normal),
    background var(--transition-fast),
    transform var(--transition-fast);
  width: 100%;
  justify-self: center;
  font-family: inherit;
}

.cmd-trigger:hover {
  border-color: rgba(8, 145, 178, 0.32);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 3px var(--color-accent-muted);
  color: var(--color-accent-deep);
}

.cmd-trigger:active {
  transform: scale(0.99);
}

.cmd-trigger:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.cmd-trigger-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.cmd-trigger-text {
  flex: 1;
  text-align: left;
  font-size: var(--text-sm);
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cmd-trigger-kbd {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 6px;
  border: 1px solid rgba(8, 145, 178, 0.16);
  background: rgba(240, 247, 251, 0.9);
  color: var(--color-text-dim);
  font-family: var(--font-family-mono);
  flex-shrink: 0;
}

.cmd-trigger-mod {
  opacity: 0.75;
}

.topbar-right {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-self: end;
}

.topbar-icon {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  border: 1px solid rgba(8, 145, 178, 0.12);
  background: rgba(255, 255, 255, 0.55);
  color: var(--color-text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    color var(--transition-fast),
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.topbar-icon:hover {
  color: var(--color-accent);
  border-color: rgba(8, 145, 178, 0.28);
  background: var(--color-accent-soft);
}

.topbar-icon:focus-visible {
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
  padding-top: 14vh;
}

.cmd-palette {
  width: 520px;
  max-height: 440px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(8, 145, 178, 0.16);
  border-radius: 18px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 24px 64px rgba(8, 116, 146, 0.18);
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
  border-bottom: 1px solid rgba(8, 145, 178, 0.1);
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

.cmd-section {
  padding-top: 4px;
}

.cmd-section-label {
  font-size: 10px;
  font-weight: var(--font-semibold);
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  padding: 8px 16px 4px;
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
  border-radius: 10px;
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
  .topbar {
    grid-template-columns: 1fr auto;
    padding-right: 12px;
  }

  .topbar-crumb {
    display: none;
  }

  .cmd-trigger {
    grid-column: 1;
    justify-self: stretch;
    max-width: none;
  }

  .topbar-right {
    grid-column: 2;
  }

  .cmd-trigger-kbd {
    display: none;
  }

  .cmd-palette {
    width: calc(100vw - 32px);
  }

  .dock {
    border-radius: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flyout-enter-active,
  .flyout-leave-active,
  .cmd-palette,
  .dock-btn,
  .dock-brand,
  .cmd-trigger {
    animation: none;
    transition: none;
  }
}
</style>
