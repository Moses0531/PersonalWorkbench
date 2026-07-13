<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deleteRoleApi,
  pageRolesApi,
  saveRoleApi,
  updateRoleApi,
  getProtectedRoleCodesApi,
  getCurrentUserRoleApi,
  listRolePermissionIdsApi,
  saveRolePermissionsApi,
  listPermissionsApi,
} from '@/apis/system/rabc'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const permDialogVisible = ref(false)
const permLoading = ref(false)
const activeTab = ref('list')
const searchQuery = ref('')

const tableData = ref([])
const form = reactive({ roleId: null, roleName: '', roleCode: '', level: null, description: '' })
const menuTreeData = ref([])
const menuTreeRef = ref(null)
const permForm = reactive({ roleId: null, permissionIds: [] })

const protectedRoleCodes = ref(new Set())
const currentUserRole = ref('')

const hierarchyLoading = ref(false)
const allRoles = ref([])

const palette = [
  { color: '#0891b2', bg: 'rgba(34, 184, 207, 0.1)', icon: '♛' },
  { color: '#8b7ec8', bg: 'rgba(139, 126, 200, 0.08)', icon: '◆' },
  { color: '#4aba6a', bg: 'rgba(74, 186, 106, 0.08)', icon: '●' },
  { color: '#5b9bd5', bg: 'rgba(91, 155, 213, 0.08)', icon: '★' },
  { color: '#0e7490', bg: 'rgba(14, 116, 144, 0.08)', icon: '▲' },
  { color: '#6a8fa3', bg: 'rgba(106, 143, 163, 0.08)', icon: '■' },
  { color: '#909399', bg: 'rgba(144, 147, 153, 0.06)', icon: '○' }
]

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

function paletteOf(index) {
  return palette[Math.min(index, palette.length - 1)]
}

const hierarchyLevels = computed(() => {
  if (!allRoles.value.length) return []
  const hasLevel = allRoles.value.filter((r) => r.level !== null && r.level !== undefined && r.level !== '')
  const noLevel = allRoles.value.filter((r) => r.level === null || r.level === undefined || r.level === '')
  const sorted = [...hasLevel].sort((a, b) => Number(a.level) - Number(b.level))
  const map = new Map()
  sorted.forEach((r) => {
    const lv = Number(r.level)
    if (!map.has(lv)) map.set(lv, [])
    map.get(lv).push(r)
  })
  const result = Array.from(map.entries()).map(([lv, roles]) => ({ level: lv, roles }))
  if (noLevel.length) result.push({ level: '未定级', roles: noLevel })
  return result
})

async function loadHierarchyData() {
  hierarchyLoading.value = true
  try {
    const res = await pageRolesApi(1, 1000)
    allRoles.value = res?.data?.records || []
  } catch (_e) {
    ElMessage.error('加载层级视图失败')
  } finally {
    hierarchyLoading.value = false
  }
}

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
    ElMessage.error('获取角色列表失败')
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
  await Promise.all([loadRoles(), loadHierarchyData(), loadRoleProtectionInfo()])
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { roleId: null, roleName: '', roleCode: '', level: null, description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  if (!canEditRole(row)) {
    ElMessage.warning('该角色受保护，无法编辑')
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
    ElMessage.success('操作成功')
    dialogVisible.value = false
    refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function removeRole(roleId) {
  try {
    await deleteRoleApi(roleId)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (_error) {
    ElMessage.error('删除失败')
  }
}

async function openPermissionAssign(row) {
  if (!canEditRole(row)) {
    ElMessage.warning('该角色受保护，无法修改权限')
    return
  }
  permDialogVisible.value = true
  permLoading.value = true
  permForm.roleId = row.roleId
  try {
    const [permissionsResp, selectedResp] = await Promise.all([
      listPermissionsApi(),
      listRolePermissionIdsApi(row.roleId)
    ])
    menuTreeData.value = buildMenuTree(permissionsResp?.data || [])
    permForm.permissionIds = (selectedResp?.data || []).map(Number)
  } finally {
    permLoading.value = false
  }
}

function buildMenuTree(permissions) {
  const map = {}
  const tree = []
  permissions.forEach((permission) => {
    map[permission.permissionId] = { ...permission, children: [] }
  })
  permissions.forEach((permission) => {
    if (permission.parentId === 0 || permission.parentId === null) {
      tree.push(map[permission.permissionId])
    } else if (map[permission.parentId]) {
      map[permission.parentId].children.push(map[permission.permissionId])
    }
  })
  return tree
}

async function submitRoleMenusWithTree() {
  permLoading.value = true
  try {
    const checkedKeys = menuTreeRef.value.getCheckedKeys()
    const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
    await saveRolePermissionsApi(permForm.roleId, [...checkedKeys, ...halfCheckedKeys])
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
  } catch (_error) {
    ElMessage.error('保存失败')
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

watch(activeTab, (t) => {
  if (t === 'hierarchy') loadHierarchyData()
})

onMounted(refreshAll)
</script>

<template>
  <div class="management-list-view">
    <div class="management-list-view__blob management-list-view__blob--1" />
    <div class="management-list-view__blob management-list-view__blob--2" />

    <div class="management-list-view__container">
      <nav class="view-mode-tabs" aria-label="角色视图切换">
        <button
          type="button"
          class="view-mode-tab"
          :class="{ active: activeTab === 'list' }"
          @click="activeTab = 'list'"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" />
            <line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
          </svg>
          列表管理
        </button>
        <button
          type="button"
          class="view-mode-tab"
          :class="{ active: activeTab === 'hierarchy' }"
          @click="activeTab = 'hierarchy'"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><path d="M12 8v4l3 3" />
          </svg>
          层级视图
        </button>
      </nav>

      <FlatManageListView
        v-show="activeTab === 'list'"
        v-model:search-query="searchQuery"
        embedded
        :show-background="false"
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
            <el-popconfirm
              v-permission="'role:remove'"
              title="确定删除该角色？"
              width="220"
              :disabled="!canDeleteRole(row)"
              @confirm="removeRole(row.roleId)"
            >
              <template #reference>
                <button type="button" class="btn-action btn-action--delete" :disabled="!canDeleteRole(row)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                </button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </FlatManageListView>

      <div v-show="activeTab === 'hierarchy'" class="role-hierarchy-view" v-loading="hierarchyLoading">
        <div v-if="hierarchyLevels.length === 0" class="role-hierarchy-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><path d="M12 8v4l3 3" />
          </svg>
          <p>暂无角色层级数据</p>
        </div>

        <div v-else class="tier-flow">
          <template v-for="(tier, idx) in hierarchyLevels" :key="tier.level">
            <div v-if="idx > 0" class="tier-connector">
              <div class="connector-line" />
              <div class="connector-arrow" />
            </div>

            <div class="tier-card" :style="{ '--tc': paletteOf(idx).color, '--tc-bg': paletteOf(idx).bg }">
              <div class="tier-header">
                <div class="tier-level-badge">{{ paletteOf(idx).icon }} LEVEL {{ tier.level }}</div>
                <span class="tier-count">{{ tier.roles.length }} 个角色</span>
              </div>
              <div class="tier-grid">
                <div v-for="role in tier.roles" :key="role.roleId" class="tier-role-card">
                  <div class="trc-icon">{{ paletteOf(idx).icon }}</div>
                  <div class="trc-body">
                    <span class="trc-name">{{ role.roleName }}</span>
                    <code class="trc-code">{{ role.roleCode }}</code>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>

  <DataOperationView
    v-model="dialogVisible"
    :title="isEdit ? '编辑角色' : '新增角色'"
    width="480px"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <el-form label-position="top" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <el-form-item label="角色名称" class="dialog-item">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" class="dialog-item">
          <el-input v-model="form.roleCode" placeholder="如 ADMIN" />
        </el-form-item>
        <el-form-item label="层级（数值越小等级越高）" class="dialog-item">
          <el-input-number v-model="form.level" :min="0" placeholder="请输入层级" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" class="dialog-item dialog-item--full">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填，描述角色用途" resize="none" />
        </el-form-item>
      </div>
    </el-form>
  </DataOperationView>

  <el-dialog v-model="permDialogVisible" title="" width="560px" class="form-dialog data-operation-view" :close-on-click-modal="false">
    <div class="perm-dialog-header">
      <h3 class="perm-dialog-title">分配权限</h3>
      <p class="perm-dialog-desc">勾选该角色可访问的权限项（目录 / 菜单 / 功能）</p>
    </div>
    <div class="perm-tree-wrap" v-loading="permLoading">
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="permissionId"
        :props="{ label: 'name', children: 'children' }"
        :default-checked-keys="permForm.permissionIds"
        class="perm-tree"
      />
    </div>
    <template #footer>
      <div class="dialog-footer">
        <button type="button" class="btn-ghost-sm" @click="permDialogVisible = false">取消</button>
        <button type="button" class="btn-primary-sm" :disabled="permLoading" @click="submitRoleMenusWithTree">
          {{ permLoading ? '保存中...' : '保存权限' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.management-list-view .role-icon-dot.dot--gold { background: #c0820a; }
.management-list-view .role-icon-dot.dot--accent { background: var(--color-accent); }
.management-list-view .role-icon-dot.dot--dim { background: var(--color-text-dim); }
.management-list-view .role-icon-dot.dot--default { background: var(--color-text-dim); }
</style>
