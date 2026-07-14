<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { deleteUserApi, pageUsersApi, saveUserApi, updateUserApi } from '@/apis/system/rabc/UserApi'
import { listRolesApi } from '@/apis/system/rabc/RoleApi'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const searchQuery = ref('')
const filteredData = ref([])
const roleOptions = ref([])

const form = reactive({
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

const columns = [
  { prop: 'id', label: 'ID', headerClass: 'th-id', cellClass: 'cell-id' },
  { prop: 'account', label: '账号' },
  { prop: 'username', label: '用户昵称', cellClass: 'cell-name' },
  { prop: 'realName', label: '真实姓名', cellClass: 'cell-muted' },
  { prop: 'phone', label: '手机号', cellClass: 'cell-muted' },
  { prop: 'email', label: '邮箱', cellClass: 'cell-muted' },
  { prop: 'roleName', label: '角色', headerClass: 'th-role' },
  { prop: 'actions', label: '操作', type: 'actions', headerClass: 'th-action', align: 'center' }
]

function resetForm() {
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

function applyFilter() {
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

function roleNameOf(row) {
  if (row?.roleName) return row.roleName
  if (row?.role?.roleName) return row.role.roleName
  if (row?.roleId == null || row?.roleId === '') return ''
  const role = roleOptions.value.find((item) => Number(item.roleId) === Number(row.roleId))
  return role?.roleName || ''
}

async function loadRoles() {
  try {
    const rolesResp = await listRolesApi()
    roleOptions.value = rolesResp?.data || []
  } catch (error) {
    const msg = String(error?.message || '')
    if (msg.includes('403') || msg.includes('无权限') || msg.includes('权限不足')) {
      roleOptions.value = []
      return
    }
    message.error(msg || '加载角色列表失败')
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await pageUsersApi(1, null)
    const data = result?.data || {}
    tableData.value = (data.records || []).map((row) => ({
      ...row,
      roleName: roleNameOf(row)
    }))
    applyFilter()
  } catch (error) {
    message.error(error.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  applyFilter()
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
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

async function submitForm() {
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
    message.error(error.message || '操作失败')
  }
}

async function removeUser(id) {
  try {
    await deleteUserApi(id)
    message.success('删除成功')
    await loadUsers()
  } catch (error) {
    message.error(error.message || '删除失败')
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
    desc="统一管理系统用户信息与角色分配"
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
        <a-popconfirm v-permission="'user:remove'" title="确认删除该用户？" @confirm="removeUser(row.id)">
          <button type="button" class="btn-action btn-action--delete" title="删除">
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
    :title="isEdit ? '编辑用户' : '新增用户'"
    width="520px"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <a-form layout="vertical" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <a-form-item v-if="!isEdit" label="账号" class="dialog-item">
          <a-input v-model:value.trim="form.account" placeholder="留空则系统自动生成" />
        </a-form-item>
        <a-form-item v-else label="账号" class="dialog-item">
          <a-input v-model:value="form.account" disabled />
        </a-form-item>

        <a-form-item label="用户昵称" class="dialog-item">
          <a-input v-model:value.trim="form.username" placeholder="请输入昵称" />
        </a-form-item>

        <a-form-item label="真实姓名" class="dialog-item">
          <a-input v-model:value.trim="form.realName" placeholder="请输入真实姓名" />
        </a-form-item>

        <a-form-item label="手机号" class="dialog-item">
          <a-input v-model:value.trim="form.phone" placeholder="请输入手机号" />
        </a-form-item>

        <a-form-item label="邮箱" class="dialog-item">
          <a-input v-model:value.trim="form.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="性别" class="dialog-item">
          <a-select v-model:value="form.sex" placeholder="请选择" style="width: 100%">
            <a-select-option value="0">男</a-select-option>
            <a-select-option value="1">女</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="角色" class="dialog-item">
          <a-select v-model:value="form.roleId" placeholder="请选择角色" style="width: 100%">
            <a-select-option
              v-for="role in roleOptions"
              :key="role.roleId"
              :value="Number(role.roleId)"
            >
              {{ role.roleName }} ({{ role.roleCode }})
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="密码" class="dialog-item dialog-item--full">
          <a-input-password v-model:value.trim="form.password" :placeholder="isEdit ? '留空则不修改' : '请设置登录密码'" />
        </a-form-item>

        <a-form-item label="备注" class="dialog-item dialog-item--full">
          <a-textarea v-model:value.trim="form.remark" :rows="2" placeholder="选填" />
        </a-form-item>
      </div>
    </a-form>
  </DataOperationView>
</template>
