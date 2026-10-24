<script setup lang="ts">
/** 表格行操作按钮：支持图标/文字样式，配合 v-permission 与 visible/disabled 动态控制 */
import { computed } from 'vue'
import type { ActionValue, RowAction } from '@/types/components'

// action 结构：{ key, permission?, label?, title?, class?, visible?, disabled?, onClick? }
// key 对应 PRESETS 中的预设图标与样式

const props = withDefaults(
  defineProps<{
    /** 操作配置数组，每项见 RowAction 结构 */
    actions?: RowAction[]
    /** 当前表格行数据 */
    row: object
    /** 'icon' | 'text' */
    variant?: string
    /** 额外上下文，传给 onClick / visible 等回调 */
    context?: object
  }>(),
  {
    actions: () => [],
    variant: 'icon',
    context: () => ({}),
  },
)

const PRESETS: Record<string, { class: string; title: string; label: string }> = {
  edit: { class: 'btn-action--edit', title: '编辑', label: '修改' },
  delete: { class: 'btn-action--delete', title: '删除', label: '删除' },
  add: { class: 'btn-action--add', title: '新增', label: '新增' },
  perm: { class: 'btn-action--perm', title: '权限', label: '权限' }
}

const isText = computed(() => props.variant === 'text')

function resolveValue(value: ActionValue, row: object, context: object): string | number | boolean | null | undefined {
  if (typeof value === 'function') return value(row, context)
  return value
}

/** 默认可见；仅当 visible 显式返回 false 时隐藏（权限仍由 v-permission 控制） */
function isVisible(action: RowAction): boolean {
  const visible = resolveValue(action.visible ?? true, props.row, props.context)
  return visible !== false
}

function isDisabled(action: RowAction): boolean {
  return Boolean(resolveValue(action.disabled ?? false, props.row, props.context))
}

/** 图标模式下用作 button title 悬停提示 */
function actionTitle(action: RowAction): string {
  const preset: { class?: string; title?: string; label?: string } = PRESETS[action.key ?? ''] || {}
  return resolveValue(action.title ?? preset.title ?? action.label ?? preset.label ?? '', props.row, props.context) as string
}

/** 文字模式下按钮展示文案 */
function actionLabel(action: RowAction): string {
  const preset: { class?: string; title?: string; label?: string } = PRESETS[action.key ?? ''] || {}
  return resolveValue(action.label ?? preset.label ?? action.title ?? preset.title ?? '', props.row, props.context) as string
}

/** 文字模式按 key 区分主次/危险色；图标模式走预设 class */
function actionClass(action: RowAction): string {
  const preset: { class?: string; title?: string; label?: string } = PRESETS[action.key ?? ''] || {}
  if (isText.value) {
    if (action.key === 'delete') return 'btn-action--danger'
    if (action.key === 'edit' || action.key === 'add') return 'btn-action--primary'
    return action.class || preset.class || 'btn-action--primary'
  }
  return action.class || preset.class || 'btn-action--edit'
}

function handleClick(action: RowAction): void {
  action.onClick?.(props.row, props.context)
}

// 先按 visible 过滤，再交给 v-permission 做权限过滤
const visibleActions = computed(() => props.actions.filter(isVisible))
</script>

<template>
  <div class="action-btns" :class="isText ? 'action-btns--text' : null">
  <!-- v-permission：无权限时 DOM 不渲染；visible：业务条件隐藏 -->
    <button
      v-for="action in visibleActions"
      :key="(action.key || action.permission || action.label) as PropertyKey"
      v-permission="action.permission"
      type="button"
      class="btn-action"
      :class="actionClass(action)"
      :disabled="isDisabled(action)"
      :title="actionTitle(action)"
      @click="handleClick(action)"
    >
      <template v-if="isText">{{ actionLabel(action) }}</template>
      <template v-else-if="action.key === 'edit'">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
      </template>
      <template v-else-if="action.key === 'delete'">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
      </template>
      <template v-else-if="action.key === 'add'">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </template>
      <template v-else-if="action.key === 'perm'">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
      </template>
      <!-- 无预设 key 时回退为文字 label -->
      <template v-else>{{ actionLabel(action) }}</template>
    </button>
  </div>
</template>
