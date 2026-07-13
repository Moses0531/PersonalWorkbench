<script setup>
/** 树形层级管理列表：展开折叠、搜索过滤后扁平化渲染 */
import { computed, ref, useSlots, watch, TransitionGroup } from 'vue'
import ManageRowActions from './ManageRowActions.vue'

const props = defineProps({
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  data: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  childrenKey: { type: String, default: 'children' },
  searchQuery: { type: String, default: '' },
  searchPlaceholder: { type: String, default: '搜索...' },
  showSearch: { type: Boolean, default: true },
  showRefresh: { type: Boolean, default: true },
  showExpandAll: { type: Boolean, default: true },
  defaultExpandAll: { type: Boolean, default: true },
  showBackground: { type: Boolean, default: true },
  showFooter: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  emptySearchText: { type: String, default: '未找到匹配结果' },
  total: { type: Number, default: null },
  rowActions: { type: Array, default: () => [] },
  actionVariant: { type: String, default: 'icon' },
  embedded: { type: Boolean, default: false },
  treeColumnProp: { type: String, default: '' }
})

const emit = defineEmits(['update:searchQuery', 'refresh', 'search', 'expand-change'])

const slots = useSlots()
// 当前展开的节点 key 集合（rowKey 解析后的值）
const expandedKeys = ref(new Set())
const allExpanded = ref(props.defaultExpandAll)

/** 树形主列：默认第一列，或通过 treeColumnProp 指定 */
const treeColumn = computed(() => {
  if (props.treeColumnProp) {
    return props.columns.find(c => c.prop === props.treeColumnProp) || props.columns[0]
  }
  return props.columns[0]
})

const flatColumns = computed(() => props.columns)

const columnCount = computed(() => flatColumns.value.length)

function resolveRowKey(row, index) {
  if (typeof props.rowKey === 'function') return props.rowKey(row, index)
  const key = row?.[props.rowKey]
  return key != null ? key : index
}

function getChildren(row) {
  const children = row?.[props.childrenKey]
  return Array.isArray(children) ? children : []
}

function hasChildren(row) {
  return getChildren(row).length > 0
}

/** 递归收集所有节点 key（用于统计总数） */
function collectAllKeys(nodes, keys = []) {
  for (const node of nodes) {
    keys.push(resolveRowKey(node))
    const children = getChildren(node)
    if (children.length) collectAllKeys(children, keys)
  }
  return keys
}

/** 仅有子节点的 key 才可展开/折叠 */
function collectExpandableKeys(nodes, keys = []) {
  for (const node of nodes) {
    const children = getChildren(node)
    if (children.length) {
      keys.push(resolveRowKey(node))
      collectExpandableKeys(children, keys)
    }
  }
  return keys
}

/** 同步「全部展开」按钮状态与 expandedKeys 是否覆盖全部可展开节点 */
function syncAllExpandedState() {
  const expandable = collectExpandableKeys(props.data)
  allExpanded.value = expandable.length > 0 && expandable.every(key => expandedKeys.value.has(key))
}

/** data 变化时按 defaultExpandAll 重置展开状态 */
function initExpandedKeys() {
  if (props.defaultExpandAll) {
    expandedKeys.value = new Set(collectExpandableKeys(props.data))
    allExpanded.value = collectExpandableKeys(props.data).length > 0
  } else {
    expandedKeys.value = new Set()
    allExpanded.value = false
  }
}

watch(() => props.data, initExpandedKeys, { immediate: true, deep: true })

function isExpanded(row) {
  return expandedKeys.value.has(resolveRowKey(row))
}

function toggleExpand(row) {
  const key = resolveRowKey(row)
  const next = new Set(expandedKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedKeys.value = next
  syncAllExpandedState()
  emit('expand-change', { key, expanded: next.has(key) })
}

function toggleExpandAll() {
  if (allExpanded.value) {
    expandedKeys.value = new Set()
    allExpanded.value = false
  } else {
    expandedKeys.value = new Set(collectExpandableKeys(props.data))
    allExpanded.value = collectExpandableKeys(props.data).length > 0
  }
}

function rowMatchesQuery(row, query) {
  // 任意列字段包含关键词即视为匹配
  const q = query.toLowerCase()
  return props.columns.some(col => {
    const val = row?.[col.prop]
    return val != null && String(val).toLowerCase().includes(q)
  })
}

// 保留匹配节点及其祖先/后代，搜索时自动展开子树
function filterTree(nodes, query) {
  if (!query.trim()) return nodes
  const result = []
  for (const node of nodes) {
    const children = filterTree(getChildren(node), query)
    if (rowMatchesQuery(node, query) || children.length) {
      result.push({ ...node, [props.childrenKey]: children })
    }
  }
  return result
}

const filteredData = computed(() => filterTree(props.data, props.searchQuery))

/**
 * 将树扁平化为带 depth 的行列表，供 tbody 逐行渲染
 * visibleRows：仅 parentExpanded 为 true 的行可见（模拟树展开）
 */
const visibleRows = computed(() => {
  const rows = []
  const query = props.searchQuery.trim()

  function walk(nodes, depth, parentExpanded) {
    for (const node of nodes) {
      const key = resolveRowKey(node)
      const childList = getChildren(node)
      // 搜索模式下强制展开，便于展示所有匹配行
      const expanded = query ? true : (parentExpanded && isExpanded(node))
      rows.push({
        row: node,
        depth,
        key,
        hasChildren: childList.length > 0,
        expanded: query ? true : isExpanded(node),
        visible: parentExpanded
      })
      if (childList.length && (query || isExpanded(node))) {
        walk(childList, depth + 1, parentExpanded && (query || isExpanded(node)))
      }
    }
  }

  walk(filteredData.value, 0, true)
  return rows.filter(r => r.visible)
})

/** 树形列表总数默认统计全部节点数（含子节点） */
const displayTotal = computed(() => {
  if (props.total != null) return props.total
  return collectAllKeys(props.data).length
})

const emptyMessage = computed(() => {
  return props.searchQuery.trim() ? props.emptySearchText : props.emptyText
})

function cellSlotName(column) {
  if (column.type === 'actions') return 'actions'
  return `cell-${column.prop}`
}

function hasCellSlot(column) {
  return Boolean(slots[cellSlotName(column)])
}

function formatCell(row, column) {
  const value = row?.[column.prop]
  if (value == null || value === '') return column.emptyText ?? '-'
  return value
}

function onSearchInput(event) {
  const value = event.target.value
  emit('update:searchQuery', value)
  emit('search', value)
}

/** 判断该列是否为树形主列（显示展开箭头与缩进） */
function isTreeCell(column) {
  return column.prop === treeColumn.value?.prop
}
</script>

<template>
  <div
    class="management-list-view hierarchical-list-view"
    :class="[
      embedded ? 'management-list-view--embedded' : null,
      loading ? 'management-list-view--loading' : null
    ]"
  >
    <template v-if="showBackground && !embedded">
      <div class="management-list-view__blob management-list-view__blob--1" />
      <div class="management-list-view__blob management-list-view__blob--2" />
    </template>

    <div class="management-list-view__container">
      <slot name="header" />
      <slot v-if="slots.stats" name="stats" />

      <div class="management-list-view__card">
        <div class="management-list-view__toolbar">
          <div class="management-list-view__toolbar-left">
            <h3 v-if="title" class="management-list-view__toolbar-title">{{ title }}</h3>
            <p v-if="desc" class="management-list-view__toolbar-desc">{{ desc }}</p>
            <slot name="toolbar-left" />
          </div>
          <div class="management-list-view__toolbar-right">
            <button
              v-if="showExpandAll"
              type="button"
              class="hierarchical-list-view__expand-btn"
              @click="toggleExpandAll"
            >
              {{ allExpanded ? '全部折叠' : '全部展开' }}
            </button>
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
              :disabled="loading"
              @click="emit('refresh')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                   :class="{ 'mlv-spin': loading }">
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
              </svg>
            </button>
            <slot name="toolbar-actions" />
          </div>
        </div>

        <div class="table-wrap">
          <div v-if="loading" class="management-list-view__loading-overlay" aria-live="polite">
            <div class="management-list-view__loading-bar" />
            <span class="management-list-view__loading-text">加载中</span>
          </div>

          <table class="data-table hierarchical-table">
            <thead>
              <tr>
                <th
                  v-for="column in flatColumns"
                  :key="column.prop"
                  :class="[
                    column.headerClass,
                    column.align === 'center' ? 'management-list-view__th--center' : null,
                    isTreeCell(column) ? 'hierarchical-table__th-tree' : null
                  ]"
                  :style="column.width ? { width: column.width } : undefined"
                >
                  {{ column.label }}
                </th>
              </tr>
            </thead>
            <tbody v-if="!loading && !visibleRows.length">
              <tr>
                <td :colspan="columnCount" class="management-list-view__empty-cell">
                  <slot name="empty" :message="emptyMessage">
                    <div class="table-empty">
                      <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
                           stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                      </svg>
                      <p>{{ emptyMessage }}</p>
                    </div>
                  </slot>
                </td>
              </tr>
            </tbody>
            <TransitionGroup
              v-else-if="!loading"
              tag="tbody"
              name="hierarchical-row"
              class="hierarchical-table__body"
            >
              <tr
                v-for="item in visibleRows"
                :key="item.key"
                class="data-row hierarchical-table__row"
                :class="{
                  'hierarchical-table__row--parent': item.hasChildren,
                  'hierarchical-table__row--child': item.depth > 0,
                  'hierarchical-table__row--expanded': item.expanded && item.hasChildren
                }"
              >
                <td
                  v-for="column in flatColumns"
                  :key="column.prop"
                  :class="[
                    column.cellClass,
                    column.align === 'center' ? 'management-list-view__td--center' : null,
                    isTreeCell(column) ? 'hierarchical-table__td-tree' : null
                  ]"
                >
                  <div
                    v-if="isTreeCell(column)"
                    class="hierarchical-table__tree-cell"
                    :style="{ '--tree-depth': item.depth }"
                  >
                    <button
                      v-if="item.hasChildren"
                      type="button"
                      class="hierarchical-table__toggle"
                      :class="{ 'is-expanded': item.expanded }"
                      :aria-expanded="item.expanded"
                      :title="item.expanded ? '折叠' : '展开'"
                      @click="toggleExpand(item.row)"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </button>
                    <span v-else class="hierarchical-table__toggle-placeholder" />

                    <span
                      class="hierarchical-table__node-icon"
                      :class="item.hasChildren ? 'hierarchical-table__node-icon--folder' : 'hierarchical-table__node-icon--leaf'"
                    >
                      <svg v-if="item.hasChildren" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                      </svg>
                      <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14 2 14 8 20 8" />
                      </svg>
                    </span>

                    <slot
                      v-if="hasCellSlot(column)"
                      :name="cellSlotName(column)"
                      :row="item.row"
                      :depth="item.depth"
                      :has-children="item.hasChildren"
                      :expanded="item.expanded"
                      :column="column"
                    />
                    <span v-else class="hierarchical-table__node-label">{{ formatCell(item.row, column) }}</span>
                  </div>

                  <template v-else>
                    <slot
                      v-if="column.type === 'actions' && slots.actions"
                      name="actions"
                      :row="item.row"
                      :depth="item.depth"
                      :has-children="item.hasChildren"
                      :expanded="item.expanded"
                      :column="column"
                    />
                    <ManageRowActions
                      v-else-if="column.type === 'actions' && rowActions.length"
                      :actions="rowActions"
                      :row="item.row"
                      :variant="actionVariant"
                      :context="{
                        depth: item.depth,
                        hasChildren: item.hasChildren,
                        expanded: item.expanded
                      }"
                    />
                    <slot
                      v-else-if="hasCellSlot(column)"
                      :name="cellSlotName(column)"
                      :row="item.row"
                      :depth="item.depth"
                      :has-children="item.hasChildren"
                      :expanded="item.expanded"
                      :column="column"
                    />
                    <template v-else>{{ formatCell(item.row, column) }}</template>
                  </template>
                </td>
              </tr>
            </TransitionGroup>
          </table>
        </div>

        <div v-if="showFooter" class="management-list-view__table-footer">
          <slot name="footer" :total="displayTotal">
            <span class="management-list-view__footer-count">共 {{ displayTotal }} 个节点</span>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>
