<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from '@/utils/message'
import { deleteUserApi, pageUsersApi, saveUserApi, updateUserApi } from '@/apis/system/rabc/UserApi'
import { listRolesApi } from '@/apis/system/rabc/RoleApi'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import type { IdLike, SysRole, UserForm, UserRow } from '@/types/entities'
import type { ColumnDef } from '@/types/components'
import type { PageResult } from '@/types/api'
import { toErrorMessage } from '@/types/api'

/** 列表行：用户 + 角色冗余字段（roleName 由 roleNameOf 补齐） */
interface UserListRow extends UserRow {
  roleName?: string
  remark?: string
  role?: { roleName?: string } | null
}

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref<UserListRow[]>([])
const searchQuery = ref('')
const filteredData = ref<UserListRow[]>([])
const roleOptions = ref<SysRole[]>([])

const sexSelectOptions = [
  { label: '男', value: '0' },
  { label: '女', value: '1' },
]

const roleSelectOptions = computed(() =>
  roleOptions.value.map((role) => ({
    label: `${role.roleName} (${role.roleCode})`,
    value: Number(role.roleId),
  })),
)

const form = reactive<UserForm & { remark: string }>({
  id: null,
  account: '',
  username: '',
  realName: '',
  phone: '',
  password: '',
  email: '',
  sex: '0',
  remark: '',
  roleId: null
})

const columns: ColumnDef[] = [
  { prop: 'id', label: 'ID', headerClass: 'th-id', cellClass: 'cell-id' },
  { prop: 'account', label: '账号' },
  { prop: 'username', label: '用户昵称', cellClass: 'cell-name' },
  { prop: 'realName', label: '真实姓名', cellClass: 'cell-muted' },
  { prop: 'phone', label: '手机号', cellClass: 'cell-muted' },
  { prop: 'email', label: '邮箱', cellClass: 'cell-muted' },
  { prop: 'roleName', label: '角色', headerClass: 'th-role' },
  { prop: 'actions', label: '操作', type: 'actions', headerClass: 'th-action', align: 'center' }
]

function resetForm(): void {
  Object.assign(form, {
    id: null,
    account: '',
    username: '',
    realName: '',
    phone: '',
    password: '',
    email: '',
    sex: '0',
    remark: '',
    roleId: null
  })
}

function applyFilter(): void {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) {
    filteredData.value = tableData.value
    return
  }
  filteredData.value = tableData.value.filter(
    (row) =>
      (row.account || '').toLowerCase().includes(q) ||
      (row.username || '').toLowerCase().includes(q) ||
      (row.phone || '').toLowerCase().includes(q) ||
      (row.email || '').toLowerCase().includes(q) ||
      roleNameOf(row).toLowerCase().includes(q)
  )
}

function roleNameOf(row: UserListRow): string {
  if (row?.roleName) return row.roleName
  if (row?.role?.roleName) return row.role.roleName
  // 受控 cast：历史数据 roleId 可能为空串，保留原判断行为
  if (row?.roleId == null || (row.roleId as unknown) === '') return ''
  const role = roleOptions.value.find((item) => Number(item.roleId) === Number(row.roleId))
  return role?.roleName || ''
}

async function loadRoles(): Promise<void> {
  try {
    const rolesResp = await listRolesApi()
    roleOptions.value = rolesResp?.data || []
  } catch (error) {
    const msg = toErrorMessage(error)
    if (msg.includes('403') || msg.includes('无权限') || msg.includes('权限不足')) {
      roleOptions.value = []
      return
    }
    message.error(msg || '加载角色列表失败')
  }
}

async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    const result = await pageUsersApi(1, null)
    const data: Partial<PageResult<UserRow>> = result?.data || {}
    tableData.value = (data.records || []).map((row) => ({
      ...row,
      roleName: roleNameOf(row)
    }))
    applyFilter()
  } catch (error) {
    message.error(toErrorMessage(error) || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  applyFilter()
}

function openCreate(): void {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserListRow): void {
  isEdit.value = true
  Object.assign(form, {
    ...row,
    roleId: row.roleId != null ? Number(row.roleId) : null,
    sex: row.sex != null && row.sex !== '' ? String(row.sex) : '0',
    remark: row.remark || '',
    password: ''
  })
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  try {
    const payload = {
      ...form,
      roleId: form.roleId != null ? Number(form.roleId) : null,
      sex: form.sex != null ? String(form.sex) : '0'
    }
    if (isEdit.value) await updateUserApi(payload)
    else await saveUserApi(payload)
    message.success('操作成功')
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    message.error(toErrorMessage(error) || '操作失败')
  }
}

async function removeUser(id: IdLike): Promise<void> {
  try {
    await deleteUserApi(id)
    message.success('删除成功')
    await loadUsers()
  } catch (error) {
    message.error(toErrorMessage(error) || '删除失败')
  }
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

<template>
  <FlatManageListView
    v-model:search-query="searchQuery"
    title="用户列表"
    desc="管理 PersonalWorkbench 的用户与角色"
    :loading="loading"
    :data="filteredData"
    :columns="columns"
    row-key="id"
    search-placeholder="搜索账号、昵称、手机..."
    empty-text="暂无用户数据"
    @refresh="loadUsers"
    @search="onSearch"
  >
    <template #toolbar-actions>
      <button v-permission="'user:add'" type="button" class="mlv-btn-primary" @click="openCreate">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        新增用户
      </button>
    </template>

    <template #cell-account="{ row }">
      <span class="account-text">{{ row.account || '-' }}</span>
    </template>

    <template #cell-roleName="{ row }">
      <span v-if="roleNameOf(row)" class="role-tag">{{ roleNameOf(row) }}</span>
      <span v-else class="cell-muted">-</span>
    </template>

    <template #actions="{ row }">
      <div class="action-btns">
        <button v-permission="'user:modify'" type="button" class="btn-action btn-action--edit" title="编辑" @click="openEdit(row)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <el-popconfirm v-permission="'user:remove'" @confirm="removeUser(row.id!)" title="确认删除该用户？">
          <template #reference>
            <button type="button" class="btn-action btn-action--delete" title="删除">
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

  <DataOperationView
    v-model="dialogVisible"
    :title="isEdit ? '编辑用户' : '新增用户'"
    width="520px"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <el-form label-position="top" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <el-form-item v-if="!isEdit" label="账号" class="dialog-item">
          <el-input v-model="form.account" placeholder="留空则系统自动生成" clearable />
        </el-form-item>
        <el-form-item v-else label="账号" class="dialog-item">
          <el-input v-model="form.account" disabled />
        </el-form-item>

        <el-form-item label="用户昵称" class="dialog-item">
          <el-input v-model="form.username" placeholder="请输入昵称" clearable />
        </el-form-item>

        <el-form-item label="真实姓名" class="dialog-item">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>

        <el-form-item label="手机号" class="dialog-item">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>

        <el-form-item label="邮箱" class="dialog-item">
          <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
        </el-form-item>

        <el-form-item label="性别" class="dialog-item">
          <el-select v-model="form.sex" placeholder="请选择" >
            <el-option v-for="o in sexSelectOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="角色" class="dialog-item">
          <el-select v-model="form.roleId" placeholder="请选择角色" >
            <el-option v-for="o in roleSelectOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="密码" class="dialog-item dialog-item--full">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改' : '请设置登录密码'"
          />
        </el-form-item>

        <el-form-item label="备注" class="dialog-item dialog-item--full">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </div>
    </el-form>
  </DataOperationView>
</template>
