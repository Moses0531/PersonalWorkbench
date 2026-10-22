<script setup>
import { computed, onMounted, reactive, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
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
import {
  Plus,
  Lock,
  Edit,
  Stamp,
  Delete,
} from '@element-plus/icons-vue'

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
const permTreeRef = ref(null)
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

function setTreeExpanded(keys) {
  treeExpandedKeys.value = keys
  const map = permTreeRef.value?.store?.nodesMap
  if (!map) return
  const keySet = new Set(keys.map(String))
  Object.values(map).forEach((node) => {
    node.expanded = keySet.has(String(node.key))
  })
}

function expandAllPermTree() {
  setTreeExpanded(collectTreeKeys(menuTreeData.value))
}

function collapseAllPermTree() {
  setTreeExpanded([])
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
  await Promise.all([loadRoles(), loadRoleProtectionInfo()])
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

async function syncPermTreeChecks() {
  await applyPermTreeChecks()
}

function onPermTreeCheck(_data, ctx) {
  treeCheckedKeys.value = ctx?.checkedKeys || []
  treeHalfCheckedKeys.value = ctx?.halfCheckedKeys || []
}

async function applyPermTreeChecks() {
  await nextTick()
  const leafKeys = filterCheckedLeafIds(permFlatList.value, permForm.permissionIds)
  treeCheckedKeys.value = leafKeys
  const leafSet = new Set(leafKeys.map(Number))
  treeHalfCheckedKeys.value = permForm.permissionIds
    .map(Number)
    .filter((id) => !Number.isNaN(id) && !leafSet.has(id))
  permTreeRef.value?.setCheckedKeys(leafKeys)
}

async function openPermissionAssign(row) {
  if (!canEditRole(row)) {
    ElMessage.warning('该角色受保护，无法修改权限')
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
      ElMessage.warning(list.length ? '权限树构建失败' : '未获取到权限数据，请检查后端接口与数据库 display_order 字段')
    }
    showPermTree.value = true
    await syncPermTreeChecks()
  } catch (error) {
    menuTreeData.value = []
    permFlatList.value = []
    treeExpandedKeys.value = []
    ElMessage.error(error.message || '加载权限树失败')
  } finally {
    permLoading.value = false
  }
}

async function submitRoleMenusWithTree() {
  permLoading.value = true
  try {
    const keys = [...treeCheckedKeys.value, ...treeHalfCheckedKeys.value]
    await saveRolePermissionsApi(permForm.roleId, keys.length ? keys : permForm.permissionIds)
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
    window.dispatchEvent(new CustomEvent('permissions-updated'))
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

onMounted(refreshAll)
</script>

<template>
  <FlatManageListView
        v-model:search-query="searchQuery"
        title="角色列表"
        desc="决定谁能看到 PersonalWorkbench 的哪些能力"
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
            <Plus />
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
            <Lock />
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
              <Edit />
            </button>
            <button
              v-permission="'role:modify'"
              type="button"
              class="btn-action btn-action--perm"
              :disabled="!canEditRole(row)"
              title="权限"
              @click="openPermissionAssign(row)"
            >
              <Stamp />
            </button>
            <el-popconfirm
              v-permission="'role:remove'"
              title="确定删除该角色？"
              :disabled="!canDeleteRole(row)"
              @confirm="removeRole(row.roleId)"
            >
          <template #reference><button type="button" class="btn-action btn-action--delete" :disabled="!canDeleteRole(row)" title="删除">
                <Delete />
              </button></template>
        </el-popconfirm>
          </div>
        </template>
  </FlatManageListView>

  <DataOperationView
    v-model="dialogVisible"
    :title="isEdit ? '编辑角色' : '新增角色'"
    :columns="2"
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
        <el-form-item label="层级（数值越小等级越高）" class="dialog-item dialog-item--full">
          <el-input-number v-model="form.level" :min="0" placeholder="请输入层级" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" class="dialog-item dialog-item--full">
          <el-input type="textarea" v-model="form.description" :rows="2" placeholder="选填，描述角色用途" />
        </el-form-item>
      </div>
    </el-form>
  </DataOperationView>

  <DataOperationView
    v-model="permDialogVisible"
    title="分配权限"
    width="720px"
    :loading="permLoading"
    confirm-text="保存权限"
    destroy-on-close
    @confirm="submitRoleMenusWithTree"
  >
    <div class="perm-dialog">
      <p class="perm-dialog-desc">勾选该角色可访问的权限项（目录 / 菜单 / 功能）</p>

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

      <div v-loading="permLoading">
        <div class="perm-tree-wrap">
          <el-tree
            v-if="showPermTree && menuTreeData.length"
            :key="`role-perm-${permForm.roleId}`"
            ref="permTreeRef"
            node-key="permissionId"
            show-checkbox
            :data="menuTreeData"
            :props="{ label: 'name', children: 'children' }"
            :default-expanded-keys="treeExpandedKeys"
            :expand-on-click-node="false"
            class="perm-tree"
            @check="onPermTreeCheck"
          >
            <template #default="{ data }">
              <span
                class="perm-tree-node"
                :class="`perm-tree-node--${getPermNodeType(data.type).cls}`"
              >
                <i
                  class="perm-tree-dot"
                  :class="`perm-tree-dot--${getPermNodeType(data.type).cls}`"
                  :title="getPermNodeType(data.type).label"
                />
                <span class="perm-tree-label">{{ data.name }}</span>
                <span v-if="data.code" class="perm-tree-code">{{ data.code }}</span>
              </span>
            </template>
          </el-tree>
          <div v-else-if="!permLoading" class="perm-tree-empty">暂无权限数据</div>
        </div>
      </div>
    </div>
  </DataOperationView>
</template>

<style scoped>
.management-list-view .role-icon-dot.dot--gold { background: #c0820a; }
.management-list-view .role-icon-dot.dot--accent { background: var(--color-accent); }
.management-list-view .role-icon-dot.dot--dim { background: var(--color-text-dim); }
.management-list-view .role-icon-dot.dot--default { background: var(--color-text-dim); }
</style>
