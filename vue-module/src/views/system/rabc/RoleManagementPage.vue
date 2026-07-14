<script setup>
import { computed, onMounted, reactive, ref, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  deleteRoleApi,
  pageRolesApi,
  saveRoleApi,
  updateRoleApi,
  getProtectedRoleCodesApi,
  getCurrentUserRoleApi,
} from '@/apis/system/rabc/RoleApi'
import { listRolePermissionIdsApi, saveRolePermissionsApi } from '@/apis/system/rabc/RolePermissionApi'
import { listPermissionsApi } from '@/apis/system/rabc/PermissionApi'
import { buildPermissionTree, filterCheckedLeafIds, permissionTypeOf } from '@/utils/menu'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const permDialogVisible = ref(false)
const permLoading = ref(false)
const searchQuery = ref('')

const tableData = ref([])
const form = reactive({ roleId: null, roleName: '', roleCode: '', level: null, description: '' })
const menuTreeData = ref([])
const permFlatList = ref([])
const showPermTree = ref(false)
const treeCheckedKeys = ref([])
const treeHalfCheckedKeys = ref([])
const treeExpandedKeys = ref([])
const permForm = reactive({ roleId: null, permissionIds: [] })

const selectedPermCount = computed(() => {
  const ids = new Set(
    [...treeCheckedKeys.value, ...treeHalfCheckedKeys.value]
      .map(Number)
      .filter((id) => !Number.isNaN(id)),
  )
  return ids.size
})

function collectTreeKeys(nodes = [], acc = []) {
  for (const node of nodes) {
    const id = Number(node.permissionId)
    if (!Number.isNaN(id)) acc.push(id)
    if (node.children?.length) collectTreeKeys(node.children, acc)
  }
  return acc
}

function expandAllPermTree() {
  treeExpandedKeys.value = collectTreeKeys(menuTreeData.value)
}

function collapseAllPermTree() {
  treeExpandedKeys.value = []
}

function getPermNodeType(type) {
  const t = permissionTypeOf({ type })
  if (t === 'D') return { label: '目录', cls: 'dir' }
  if (t === 'M') return { label: '菜单', cls: 'menu' }
  if (t === 'F') return { label: '功能', cls: 'func' }
  return { label: t || '-', cls: 'dir' }
}

const protectedRoleCodes = ref(new Set())
const currentUserRole = ref('')

const columns = [
  { prop: 'roleId', label: 'ID', headerClass: 'th-id', cellClass: 'cell-id' },
  { prop: 'roleName', label: '角色名称' },
  { prop: 'roleCode', label: '编码' },
  { prop: 'level', label: '层级', headerClass: 'th-level' },
  { prop: 'status', label: '状态', headerClass: 'th-status' },
  { prop: 'description', label: '描述' },
  { prop: 'actions', label: '操作', type: 'actions', headerClass: 'th-action', align: 'center' }
]

const filteredData = ref([])

function isRoleProtected(role) {
  return protectedRoleCodes.value.has(String(role.roleCode || '').toUpperCase())
}

function canEditRole(role) {
  if (currentUserRole.value === 'ROOT') return true
  return !isRoleProtected(role)
}

function canDeleteRole(role) {
  if (currentUserRole.value === 'ROOT') return true
  return !isRoleProtected(role)
}

async function loadRoles() {
  loading.value = true
  try {
    const result = await pageRolesApi(1, null)
    const data = result?.data || {}
    tableData.value = data.records || []
    applyFilter()
  } catch (_error) {
    message.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) {
    filteredData.value = tableData.value
    return
  }
  filteredData.value = tableData.value.filter(
    (row) =>
      (row.roleName || '').toLowerCase().includes(q) ||
      (row.roleCode || '').toLowerCase().includes(q) ||
      (row.description || '').toLowerCase().includes(q)
  )
}

async function loadRoleProtectionInfo() {
  try {
    const [protectedResp, currentRoleResp] = await Promise.all([
      getProtectedRoleCodesApi(),
      getCurrentUserRoleApi()
    ])
    const codes = protectedResp?.data || []
    protectedRoleCodes.value = new Set(codes.map((code) => String(code).toUpperCase()))
    currentUserRole.value = String(currentRoleResp?.data || '').toUpperCase()
  } catch (_error) {
    /* ignore */
  }
}

async function refreshAll() {
  await Promise.all([loadRoles(), loadRoleProtectionInfo()])
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { roleId: null, roleName: '', roleCode: '', level: null, description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  if (!canEditRole(row)) {
    message.warning('该角色受保护，无法编辑')
    return
  }
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submitForm() {
  try {
    if (isEdit.value) await updateRoleApi(form)
    else await saveRoleApi(form)
    message.success('操作成功')
    dialogVisible.value = false
    refreshAll()
  } catch (error) {
    message.error(error.message || '操作失败')
  }
}

async function removeRole(roleId) {
  try {
    await deleteRoleApi(roleId)
    message.success('删除成功')
    refreshAll()
  } catch (_error) {
    message.error('删除失败')
  }
}

async function syncPermTreeChecks() {
  await nextTick()
  const leafKeys = filterCheckedLeafIds(permFlatList.value, permForm.permissionIds)
  treeCheckedKeys.value = leafKeys
  const leafSet = new Set(leafKeys.map(Number))
  treeHalfCheckedKeys.value = permForm.permissionIds
    .map(Number)
    .filter((id) => !Number.isNaN(id) && !leafSet.has(id))
}

function onPermTreeCheck(checkedKeys, e) {
  treeCheckedKeys.value = checkedKeys
  treeHalfCheckedKeys.value = e?.halfCheckedKeys || []
}

function onPermDialogOpenChange(open) {
  if (open && showPermTree.value) syncPermTreeChecks()
}

async function openPermissionAssign(row) {
  if (!canEditRole(row)) {
    message.warning('该角色受保护，无法修改权限')
    return
  }
  showPermTree.value = false
  permDialogVisible.value = true
  permLoading.value = true
  menuTreeData.value = []
  permFlatList.value = []
  treeExpandedKeys.value = []
  treeCheckedKeys.value = []
  treeHalfCheckedKeys.value = []
  permForm.roleId = row.roleId
  permForm.permissionIds = []
  try {
    const [permissionsResp, selectedResp] = await Promise.all([
      listPermissionsApi(),
      listRolePermissionIdsApi(row.roleId),
    ])
    const list = permissionsResp?.data || []
    permFlatList.value = list
    permForm.permissionIds = (selectedResp?.data || []).map(Number).filter((id) => !Number.isNaN(id))
    menuTreeData.value = buildPermissionTree(list)
    treeExpandedKeys.value = collectTreeKeys(menuTreeData.value)
    if (!menuTreeData.value.length) {
      message.warning(list.length ? '权限树构建失败' : '未获取到权限数据，请检查后端接口与数据库 display_order 字段')
    }
    showPermTree.value = true
    await syncPermTreeChecks()
  } catch (error) {
    menuTreeData.value = []
    permFlatList.value = []
    treeExpandedKeys.value = []
    message.error(error.message || '加载权限树失败')
  } finally {
    permLoading.value = false
  }
}

async function submitRoleMenusWithTree() {
  permLoading.value = true
  try {
    const keys = [...treeCheckedKeys.value, ...treeHalfCheckedKeys.value]
    await saveRolePermissionsApi(permForm.roleId, keys.length ? keys : permForm.permissionIds)
    message.success('权限保存成功')
    permDialogVisible.value = false
    window.dispatchEvent(new CustomEvent('permissions-updated'))
  } catch (_error) {
    message.error('保存失败')
  } finally {
    permLoading.value = false
  }
}

function getLevelColor(level) {
  if (level === null || level === undefined) return 'default'
  const n = Number(level)
  if (n <= 0) return 'gold'
  if (n <= 2) return 'accent'
  return 'dim'
}

onMounted(refreshAll)
</script>

<template>
  <FlatManageListView
        v-model:search-query="searchQuery"
        title="角色列表"
        desc="管理系统访问权限的核心载体"
        :loading="loading"
        :data="filteredData"
        :columns="columns"
        row-key="roleId"
        search-placeholder="搜索角色名称、编码..."
        empty-text="暂无角色数据"
        @refresh="refreshAll"
        @search="applyFilter"
      >
        <template #toolbar-actions>
          <button v-permission="'role:add'" type="button" class="mlv-btn-primary" @click="openCreate">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新增角色
          </button>
        </template>

        <template #cell-roleName="{ row }">
          <div class="cell-role-name">
            <div class="role-icon-dot" :class="`dot--${getLevelColor(row.level)}`" />
            <span class="cell-name">{{ row.roleName || '-' }}</span>
          </div>
        </template>

        <template #cell-roleCode="{ row }">
          <code class="code-tag">{{ row.roleCode || '-' }}</code>
        </template>

        <template #cell-level="{ row }">
          <span v-if="row.level !== null && row.level !== undefined" class="level-badge" :class="`level-badge--${getLevelColor(row.level)}`">
            LV-{{ row.level }}
          </span>
          <span v-else class="cell-muted">-</span>
        </template>

        <template #cell-status="{ row }">
          <span v-if="isRoleProtected(row)" class="status-tag status-tag--protected">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            受保护
          </span>
          <span v-else class="status-tag status-tag--normal">
            <span class="normal-dot" />
            正常
          </span>
        </template>

        <template #cell-description="{ row }">
          <span class="cell-muted">{{ row.description || '-' }}</span>
        </template>

        <template #actions="{ row }">
          <div class="action-btns">
            <button
              v-permission="'role:modify'"
              type="button"
              class="btn-action btn-action--edit"
              :disabled="!canEditRole(row)"
              title="编辑"
              @click="openEdit(row)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
            </button>
            <button
              v-permission="'role:modify'"
              type="button"
              class="btn-action btn-action--perm"
              :disabled="!canEditRole(row)"
              title="权限"
              @click="openPermissionAssign(row)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </button>
            <a-popconfirm
              v-permission="'role:remove'"
              title="确定删除该角色？"
              :disabled="!canDeleteRole(row)"
              @confirm="removeRole(row.roleId)"
            >
              <button type="button" class="btn-action btn-action--delete" :disabled="!canDeleteRole(row)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            </a-popconfirm>
          </div>
        </template>
  </FlatManageListView>

  <DataOperationView
    v-model="dialogVisible"
    :title="isEdit ? '编辑角色' : '新增角色'"
    width="480px"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <a-form layout="vertical" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <a-form-item label="角色名称" class="dialog-item">
          <a-input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" class="dialog-item">
          <a-input v-model:value="form.roleCode" placeholder="如 ADMIN" />
        </a-form-item>
        <a-form-item label="层级（数值越小等级越高）" class="dialog-item">
          <a-input-number v-model:value="form.level" :min="0" placeholder="请输入层级" style="width: 100%" />
        </a-form-item>
        <a-form-item label="描述" class="dialog-item dialog-item--full">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="选填，描述角色用途" />
        </a-form-item>
      </div>
    </a-form>
  </DataOperationView>

  <a-modal
    v-model:open="permDialogVisible"
    :title="null"
    width="600px"
    class="form-dialog data-operation-view perm-assign-dialog"
    :mask-closable="false"
    destroy-on-close
    :footer="null"
    @after-open-change="onPermDialogOpenChange"
  >
    <div class="perm-dialog">
      <div class="perm-dialog-header">
        <div class="perm-dialog-heading">
          <span class="perm-dialog-icon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
          </span>
          <div class="perm-dialog-heading-text">
            <h3 class="perm-dialog-title">分配权限</h3>
            <p class="perm-dialog-desc">勾选该角色可访问的权限项（目录 / 菜单 / 功能）</p>
          </div>
        </div>
      </div>

      <div class="perm-tree-toolbar">
        <div class="perm-tree-toolbar-meta">
          <span class="perm-tree-count">已选 <em>{{ selectedPermCount }}</em> 项</span>
          <span class="perm-tree-legend" aria-hidden="true">
            <span class="perm-tree-legend-item"><i class="perm-tree-dot perm-tree-dot--dir" />目录</span>
            <span class="perm-tree-legend-item"><i class="perm-tree-dot perm-tree-dot--menu" />菜单</span>
            <span class="perm-tree-legend-item"><i class="perm-tree-dot perm-tree-dot--func" />功能</span>
          </span>
        </div>
        <div class="perm-tree-toolbar-actions">
          <button type="button" class="perm-tree-link-btn" :disabled="!menuTreeData.length || permLoading" @click="expandAllPermTree">
            全部展开
          </button>
          <span class="perm-tree-toolbar-sep" aria-hidden="true" />
          <button type="button" class="perm-tree-link-btn" :disabled="!menuTreeData.length || permLoading" @click="collapseAllPermTree">
            全部折叠
          </button>
        </div>
      </div>

      <a-spin :spinning="permLoading">
        <div class="perm-tree-wrap">
          <a-tree
            v-if="showPermTree && menuTreeData.length"
            :key="`role-perm-${permForm.roleId}`"
            v-model:expandedKeys="treeExpandedKeys"
            checkable
            block-node
            :selectable="false"
            :tree-data="menuTreeData"
            :checked-keys="treeCheckedKeys"
            :field-names="{ children: 'children', title: 'name', key: 'permissionId' }"
            class="perm-tree"
            @check="onPermTreeCheck"
          >
            <template #switcherIcon="{ expanded, isLeaf }">
              <span v-if="isLeaf" class="perm-tree-leaf-gap" />
              <span
                v-else
                class="perm-tree-chevron"
                :class="{ 'perm-tree-chevron--open': expanded }"
                aria-hidden="true"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </span>
            </template>
            <template #title="node">
              <span
                class="perm-tree-node"
                :class="`perm-tree-node--${getPermNodeType(node.type).cls}`"
              >
                <i
                  class="perm-tree-dot"
                  :class="`perm-tree-dot--${getPermNodeType(node.type).cls}`"
                  :title="getPermNodeType(node.type).label"
                />
                <span class="perm-tree-label">{{ node.name || node.title }}</span>
                <span v-if="node.code" class="perm-tree-code">{{ node.code }}</span>
              </span>
            </template>
          </a-tree>
          <div v-else-if="!permLoading" class="perm-tree-empty">暂无权限数据</div>
        </div>
      </a-spin>

      <div class="dialog-footer perm-dialog-footer">
        <button type="button" class="btn-ghost-sm" @click="permDialogVisible = false">取消</button>
        <button type="button" class="btn-primary-sm" :disabled="permLoading" @click="submitRoleMenusWithTree">
          {{ permLoading ? '保存中...' : '保存权限' }}
        </button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.management-list-view .role-icon-dot.dot--gold { background: #c0820a; }
.management-list-view .role-icon-dot.dot--accent { background: var(--color-accent); }
.management-list-view .role-icon-dot.dot--dim { background: var(--color-text-dim); }
.management-list-view .role-icon-dot.dot--default { background: var(--color-text-dim); }
</style>
