<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deletePermissionApi,
  listPermissionsApi,
  savePermissionApi,
  updatePermissionApi,
} from '@/apis/system/rabc/PermissionApi'
import { assignPermissionRolesApi, listPermissionRoleIdsApi } from '@/apis/system/rabc/RolePermissionApi'
import { listRolesApi } from '@/apis/system/rabc/RoleApi'
import {
  componentPathFromRouterName,
  componentPathOf,
  isDisplayOf,
  normalizePermissionList,
  permissionTypeOf,
  viewFileFromRouterName,
  buildPermissionTree,
} from '@/utils/menu'
import HierarchicalManageListView from '@/components/ListView/HierarchicalManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const searchQuery = ref('')

const roleOptions = ref([])
const roleAssignLoading = ref(false)

const form = reactive({
  permissionId: null,
  name: '',
  parentId: 0,
  type: 'D',
  routerName: '',
  isDisplay: 0,
  status: '0',
  code: '',
  icon: '',
  displayOrder: 0,
  remark: '',
  roleIds: []
})

const columns = [
  { prop: 'name', label: '名称' },
  { prop: 'type', label: '类型', width: '56px' },
  { prop: 'routerName', label: '路由名称', width: '130px' },
  { prop: 'componentPath', label: '访问路径', width: '150px' },
  { prop: 'code', label: '权限标识', width: '100px' },
  { prop: 'isDisplay', label: '显示', width: '72px', align: 'center' },
  { prop: 'status', label: '状态', width: '72px', align: 'center' },
  { prop: 'displayOrder', label: '排序', width: '56px', align: 'center' },
  { prop: 'remark', label: '备注', width: '110px' },
  { prop: 'actions', label: '操作', type: 'actions', width: '108px', align: 'center' }
]

const previewUrlPath = computed(() => componentPathFromRouterName(form.routerName))
const previewViewFile = computed(() => viewFileFromRouterName(form.routerName))

const parentOptions = computed(() => {
  const editingId = form.permissionId
  const isFunction = form.type === 'F'
  const list = tableData.value.filter((item) => {
    if (editingId && Number(item.permissionId) === Number(editingId)) return false
    const type = permissionTypeOf(item)
    return isFunction ? type === 'M' : type === 'D'
  })
  const options = [{ value: 0, label: '根节点' }]
  list
    .slice()
    .sort((a, b) => (a.displayOrder ?? 99999) - (b.displayOrder ?? 99999) || (a.permissionId || 0) - (b.permissionId || 0))
    .forEach((item) => {
      options.push({
        value: Number(item.permissionId),
        label: `${item.name} (#${item.permissionId})`
      })
    })
  return options
})

const treeData = computed(() => buildPermissionTree(tableData.value))

const roleSelectOptions = computed(() =>
  roleOptions.value.map((role) => ({
    value: Number(role.roleId),
    label: `${role.roleName}（${role.roleCode}）`
  }))
)

async function loadPermissions() {
  loading.value = true
  try {
    const result = await listPermissionsApi()
    const list = Array.isArray(result?.data) ? result.data : []
    tableData.value = normalizePermissionList(list)
  } catch (error) {
    ElMessage.error(error.message || '获取权限列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    permissionId: null,
    name: '',
    parentId: 0,
    type: 'D',
    routerName: '',
    isDisplay: 0,
    status: '0',
    code: '',
    icon: '',
    displayOrder: 0,
    remark: '',
    roleIds: []
  })
}

async function loadRoles() {
  try {
    const res = await listRolesApi()
    roleOptions.value = Array.isArray(res?.data) ? res.data : []
  } catch (error) {
    const msg = String(error?.message || '')
    if (!msg.includes('403') && !msg.includes('无权限')) {
      ElMessage.error(msg || '加载角色列表失败')
    }
    roleOptions.value = []
  }
}

async function loadPermissionRoleIds(permissionId) {
  if (!permissionId) {
    form.roleIds = []
    return
  }
  roleAssignLoading.value = true
  try {
    const res = await listPermissionRoleIdsApi(permissionId)
    form.roleIds = (res?.data || []).map(Number)
  } catch (_error) {
    form.roleIds = []
  } finally {
    roleAssignLoading.value = false
  }
}

async function openCreate(parentId = 0) {
  isEdit.value = false
  resetForm()
  form.parentId = parentId
  dialogVisible.value = true
  if (parentId > 0) {
    await loadPermissionRoleIds(parentId)
  }
}

async function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    permissionId: row.permissionId,
    name: row.name || '',
    parentId: row.parentId ?? 0,
    type: permissionTypeOf(row) || 'D',
    routerName: row.routerName || '',
    isDisplay: row.isDisplay ?? row.is_display ?? (row.visible === '1' ? 1 : 0),
    status: row.status ?? '0',
    code: row.code || '',
    icon: row.icon || '',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
    roleIds: []
  })
  dialogVisible.value = true
  await loadPermissionRoleIds(row.permissionId)
}

function permissionPayloadFromForm() {
  const { roleIds: _roleIds, ...permission } = form
  return permission
}

function findCreatedPermissionId(list, payload) {
  const match = list.find(
    (item) =>
      item.name === payload.name &&
      Number(item.parentId) === Number(payload.parentId) &&
      permissionTypeOf(item) === String(payload.type || 'D').toUpperCase() &&
      (item.code || '') === (payload.code || '')
  )
  return match?.permissionId ?? null
}

async function submitForm() {
  try {
    const payload = permissionPayloadFromForm()
    let permissionId = form.permissionId
    if (isEdit.value) {
      await updatePermissionApi(payload)
    } else {
      await savePermissionApi(payload)
      const refreshed = await listPermissionsApi()
      permissionId = findCreatedPermissionId(refreshed?.data || [], payload)
    }
    if (!permissionId) {
      ElMessage.warning('权限已保存，但未能分配角色，请编辑该权限重新勾选角色')
      dialogVisible.value = false
      await loadPermissions()
      return
    }
    await assignPermissionRolesApi(permissionId, form.roleIds.map(Number))
    ElMessage.success(isEdit.value ? '权限与角色已保存' : '权限已创建并完成角色分配')
    dialogVisible.value = false
    await loadPermissions()
    window.dispatchEvent(new CustomEvent('permissions-updated'))
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

async function removePermission(permissionId) {
  try {
    await deletePermissionApi(permissionId)
    ElMessage.success('删除成功')
    await loadPermissions()
    window.dispatchEvent(new CustomEvent('permissions-updated'))
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function getNodeType(type) {
  const t = String(type || '').trim().toUpperCase()
  if (t === 'D') return { label: '目录', cls: 'dir' }
  if (t === 'M') return { label: '菜单', cls: 'menu' }
  if (t === 'F') return { label: '功能', cls: 'func' }
  return { label: type || '-', cls: 'dir' }
}

onMounted(async () => {
  await Promise.all([loadPermissions(), loadRoles()])
})
</script>

<template>
  <HierarchicalManageListView
    v-model:search-query="searchQuery"
    title="权限列表"
    desc="维护菜单、路由与功能权限标识"
    :loading="loading"
    :data="treeData"
    :columns="columns"
    row-key="permissionId"
    tree-column-prop="name"
    search-placeholder="搜索权限名、路由、标识..."
    empty-text="暂无权限数据"
    @refresh="loadPermissions"
  >
    <template #toolbar-actions>
      <button v-permission="'permission:add'" type="button" class="mlv-btn-primary" @click="openCreate(0)">
        <Plus />
        新增根权限
      </button>
    </template>

    <template #cell-name="{ row }">
      <span class="type-dot" :class="`type-dot--${getNodeType(row.type).cls}`" />
      <span class="perm-name-text">{{ row.name }}</span>
    </template>

    <template #cell-type="{ row }">
      <span class="type-badge" :class="`type-badge--${getNodeType(row.type).cls}`">
        {{ permissionTypeOf(row) || '-' }}
      </span>
    </template>

    <template #cell-routerName="{ row }">
      <code v-if="row.routerName" class="code-tag code-tag--green">{{ row.routerName }}</code>
      <span v-else class="cell-empty">-</span>
    </template>

    <template #cell-componentPath="{ row }">
      <code v-if="componentPathOf(row)" class="code-tag code-tag--purple">{{ componentPathOf(row) }}</code>
      <span v-else class="cell-empty">-</span>
    </template>

    <template #cell-code="{ row }">
      <span v-if="row.code" class="perm-badge">{{ row.code }}</span>
      <span v-else class="cell-empty">-</span>
    </template>

    <template #cell-isDisplay="{ row }">
      <span class="dot-indicator" :class="isDisplayOf(row) === 0 ? 'dot-indicator--green' : 'dot-indicator--red'" />
      {{ isDisplayOf(row) === 0 ? '显示' : '隐藏' }}
    </template>

    <template #cell-status="{ row }">
      <span class="dot-indicator" :class="row.status === '0' ? 'dot-indicator--green' : 'dot-indicator--red'" />
      {{ row.status === '0' ? '正常' : '停用' }}
    </template>

    <template #cell-displayOrder="{ row }">
      <span class="cell-order">{{ row.displayOrder ?? 0 }}</span>
    </template>

    <template #cell-remark="{ row }">
      <span class="cell-remark">{{ row.remark || '-' }}</span>
    </template>

    <template #actions="{ row }">
      <div class="action-btns">
        <button v-permission="'permission:modify'" type="button" class="btn-action btn-action--edit" title="编辑" @click="openEdit(row)">
          <Edit />
        </button>
        <button v-permission="'permission:add'" type="button" class="btn-action btn-action--add" title="新增子项" @click="openCreate(row.permissionId)">
          <Plus />
        </button>
        <el-popconfirm v-permission="'permission:remove'" title="确认删除该权限？" @confirm="removePermission(row.permissionId)">
          <template #reference><button type="button" class="btn-action btn-action--delete" title="删除">
            <Delete />
          </button></template>
        </el-popconfirm>
      </div>
    </template>

    <template #footer>
      <span class="management-list-view__footer-count">共 {{ tableData.length }} 个权限节点</span>
    </template>
  </HierarchicalManageListView>

  <DataOperationView
    v-model="dialogVisible"
    :title="isEdit ? '编辑权限' : '新增权限'"
    :columns="2"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <el-form label-position="top" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <el-form-item label="名称" class="dialog-item" required>
          <el-input v-model.trim="form.name" placeholder="显示名称" />
        </el-form-item>

        <el-form-item label="类型（D/M/F）" class="dialog-item">
          <el-select v-model="form.type" style="width: 100%">
            <el-option value="D">D - 目录</el-option>
            <el-option value="M">M - 菜单</el-option>
            <el-option value="F">F - 功能</el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="上级（parent_id）" class="dialog-item">
          <el-select
            v-model="form.parentId"
            style="width: 100%"
            filterable
            placeholder="选择上级目录或菜单"
          >
            <el-option
              v-for="opt in parentOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <p class="field-hint">
            {{ form.type === 'F' ? '功能的上级须为 M 类型菜单页' : '目录/菜单的上级为根节点或 D 类型目录' }}
          </p>
        </el-form-item>

        <el-form-item label="排序（display_order）" class="dialog-item">
          <el-input-number v-model="form.displayOrder" :min="0" :max="9999" style="width: 100%" />
          <p class="field-hint">数值越小越靠前</p>
        </el-form-item>

        <el-form-item label="路由名称（router_name）" class="dialog-item">
          <el-input v-model.trim="form.routerName" placeholder="如 DashboardPage（PascalCase，以 Page 结尾）" clearable />
          <p v-if="form.type === 'M' && form.routerName" class="field-hint">
            将自动生成访问路径 <code>{{ previewUrlPath }}</code>，页面组件 <code>{{ previewViewFile }}</code>
          </p>
          <p v-else-if="form.type === 'M'" class="field-hint">M 类型填写 router_name，component_path 由系统自动解析</p>
        </el-form-item>

        <el-form-item label="权限标识（code）" class="dialog-item">
          <el-input v-model.trim="form.code" placeholder="如 user:add" clearable />
          <p class="field-hint">与后端 @PreAuthorize 及 v-permission 对应</p>
        </el-form-item>

        <el-form-item label="图标（icon）" class="dialog-item">
          <el-input v-model.trim="form.icon" placeholder="图标 URL，选填" />
        </el-form-item>

        <el-form-item label="是否显示（is_display）" class="dialog-item">
          <el-select v-model="form.isDisplay" style="width: 100%">
            <el-option :value="0">0 - 显示</el-option>
            <el-option :value="1">1 - 隐藏</el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="状态（status）" class="dialog-item">
          <el-select v-model="form.status" style="width: 100%">
            <el-option value="0">0 - 正常</el-option>
            <el-option value="1">1 - 停用</el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="备注（remark）" class="dialog-item">
          <el-input v-model.trim="form.remark" placeholder="选填" />
        </el-form-item>

        <el-form-item label="角色权限分配" class="dialog-item dialog-item--full">
          <div v-loading="roleAssignLoading">
            <div class="role-assign-box">
              <p class="field-hint role-assign-hint">
                选择可访问该权限的角色，保存后写入 role_permission；新增子项时继承父级已授权角色。
              </p>
              <el-select
                v-if="roleOptions.length"
                v-model="form.roleIds"
                multiple
                clearable
                filterable
                placeholder="请选择角色"
                style="width: 100%"
              >
                <el-option
                  v-for="opt in roleSelectOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <p v-else class="role-assign-empty">暂无可选角色，请先在角色管理中创建角色</p>
            </div>
          </div>
        </el-form-item>
      </div>
    </el-form>
  </DataOperationView>
</template>
