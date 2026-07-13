<script setup>
/** 扁平数据管理列表通用布局：工具栏、KPI、表格与行操作插槽 */
import { computed, useSlots } from 'vue'
import TotalNumberView from './TotalNumberView.vue'
import ManageRowActions from './ManageRowActions.vue'

const props = defineProps({
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  data: { type: Array, default: () => [] },           // 已过滤后的行数据，由父组件维护
  columns: { type: Array, default: () => [] },       // 列定义：prop / label / type / width / align 等
  rowKey: { type: [String, Function], default: 'id' },
  searchQuery: { type: String, default: '' },        // 支持 v-model:search-query
  searchPlaceholder: { type: String, default: '搜索...' },
  showSearch: { type: Boolean, default: true },
  showRefresh: { type: Boolean, default: true },
  showBackground: { type: Boolean, default: true },
  showFooter: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  emptySearchText: { type: String, default: '未找到匹配结果' },
  total: { type: Number, default: null },            // 显式总数；null 时用 data.length
  totalNumbers: { type: Array, default: () => [] },  // KPI 卡片数据，传给 TotalNumberView
  rowActions: { type: Array, default: () => [] },    // 默认行操作，列 type='actions' 时使用
  actionVariant: { type: String, default: 'icon' },
  variant: { type: String, default: '' },              // 附加样式变体 class 后缀
  embedded: { type: Boolean, default: false },         // 嵌入模式：无背景 blob、紧凑布局
  rowClass: { type: Function, default: null },
  rowStyle: { type: Function, default: null }
})

const emit = defineEmits(['update:searchQuery', 'refresh', 'search'])

const slots = useSlots()

const columnCount = computed(() => props.columns.length)

/** 页脚展示条数：优先 props.total，否则当前 data 长度 */
const displayTotal = computed(() => {
  if (props.total != null) return props.total
  return props.data.length
})

/** 有搜索词时展示「未找到」文案，否则展示空数据文案 */
const emptyMessage = computed(() => {
  return props.searchQuery.trim() ? props.emptySearchText : props.emptyText
})

function resolveRowKey(row, index) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, index)
  const key = row?.[props.rowKey]
  return key != null ? key : index
}

/** 自定义列插槽名：普通列 cell-{prop}，操作列固定 actions */
function cellSlotName(column) {
  if (column.type === 'actions') return 'actions'
  return `cell-${column.prop}`
}

function hasCellSlot(column) {
  const name = cellSlotName(column)
  return Boolean(slots[name])
}

/** 默认单元格展示：空值显示 '-' 或 column.emptyText */
function formatCell(row, column) {
  const value = row?.[column.prop]
  if (value == null || value === '') return column.emptyText ?? '-'
  return value
}

/** 同步 v-model 并通知父组件执行过滤（父组件通常在 @search 里 filter） */
function onSearchInput(event) {
  const value = event.target.value
  emit('update:searchQuery', value)
  emit('search', value)
}
</script>

<template>
  <div
    class="management-list-view"
    :class="[
      variant ? `management-list-view--${variant}` : null,
      embedded ? 'management-list-view--embedded' : null
    ]"
  >
    <!-- 装饰性背景，embedded 时可关闭 -->
    <template v-if="showBackground && !embedded">
      <div class="management-list-view__blob management-list-view__blob--1" />
      <div class="management-list-view__blob management-list-view__blob--2" />
    </template>

    <div class="management-list-view__container">
      <slot name="header" />

      <!-- stats 插槽优先；否则用 totalNumbers 渲染 KPI -->
      <slot v-if="slots.stats" name="stats" />
      <TotalNumberView v-else-if="totalNumbers.length" :items="totalNumbers" />

      <div class="management-list-view__card">
        <!-- 工具栏：标题 + 搜索 + 刷新 + toolbar-actions 插槽（如「新增」按钮） -->
        <div class="management-list-view__toolbar">
          <div class="management-list-view__toolbar-left">
            <h3 v-if="title" class="management-list-view__toolbar-title">{{ title }}</h3>
            <p v-if="desc" class="management-list-view__toolbar-desc">{{ desc }}</p>
            <slot name="toolbar-left" />
          </div>
          <div class="management-list-view__toolbar-right">
            <div v-if="showSearch" class="management-list-view__search">
              <svg class="management-list-view__search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                :value="searchQuery"
                type="text"
                class="management-list-view__search-input"
                :placeholder="searchPlaceholder"
                @input="onSearchInput"
              />
            </div>
            <button
              v-if="showRefresh"
              type="button"
              class="management-list-view__btn-icon"
              title="刷新"
              @click="emit('refresh')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
              </svg>
            </button>
            <slot name="toolbar-actions" />
          </div>
        </div>

        <slot name="table-prefix" />

        <div class="table-wrap" v-loading="loading">
          <table class="data-table">
            <thead>
              <tr>
                <th
                  v-for="column in columns"
                  :key="column.prop"
                  :class="[
                    column.headerClass,
                    column.align === 'center' ? 'management-list-view__th--center' : null
                  ]"
                  :style="column.width ? { width: column.width } : undefined"
                >
                  {{ column.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!data.length">
                <td :colspan="columnCount" class="management-list-view__empty-cell">
                  <slot name="empty" :message="emptyMessage">
                    <div class="table-empty">
                      <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
                           stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                        <circle cx="9" cy="7" r="4" />
                      </svg>
                      <p>{{ emptyMessage }}</p>
                    </div>
                  </slot>
                </td>
              </tr>
              <template v-else>
                <!-- body 插槽可完全替换 tbody 内容；默认按 columns 渲染 -->
                <slot name="body" :data="data">
                  <tr
                    v-for="(row, index) in data"
                    :key="resolveRowKey(row, index)"
                    class="data-row"
                    :class="rowClass ? rowClass(row, index) : undefined"
                    :style="rowStyle ? rowStyle(row, index) : undefined"
                  >
                    <slot name="row" :row="row" :index="index">
                      <td
                        v-for="column in columns"
                        :key="column.prop"
                        :class="[
                          column.cellClass,
                          column.align === 'center' ? 'management-list-view__td--center' : null
                        ]"
                      >
                        <!-- 操作列优先级：actions 插槽 > rowActions 默认按钮 > 普通插槽 > formatCell -->
                        <slot
                          v-if="column.type === 'actions' && slots.actions"
                          name="actions"
                          :row="row"
                          :index="index"
                          :column="column"
                        />
                        <ManageRowActions
                          v-else-if="column.type === 'actions' && rowActions.length"
                          :actions="rowActions"
                          :row="row"
                          :variant="actionVariant"
                          :context="{ index }"
                        />
                        <slot
                          v-else-if="hasCellSlot(column)"
                          :name="cellSlotName(column)"
                          :row="row"
                          :index="index"
                          :column="column"
                        />
                        <template v-else>{{ formatCell(row, column) }}</template>
                      </td>
                    </slot>
                  </tr>
                </slot>
              </template>
            </tbody>
          </table>
        </div>

        <div v-if="showFooter" class="management-list-view__table-footer">
          <slot name="footer" :total="displayTotal">
            <span class="management-list-view__footer-count">共 {{ displayTotal }} 条记录</span>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>
