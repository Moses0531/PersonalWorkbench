<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deleteUserApi,
  pageAdminsApi,
  saveUserApi,
  updateUserApi,
  listRolesApi,
} from '@/apis/system/rabc'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const roleOptions = ref([])
const searchQuery = ref('')
const filteredData = ref([])

const form = reactive({
  id: null,
  account: '',
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  sex: '0',
  remark: '',
  roleId: null
})

const columns = [
  { prop: 'account', label: '账号' },
  { prop: 'username', label: '用户昵称', cellClass: 'cell-name' },
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
    email: '',
    password: '',
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
      (row.roleName || '').toLowerCase().includes(q)
  )
}

function enrichAdminRow(row) {
  const role = roleOptions.value.find((item) => Number(item.roleId) === Number(row.roleId))
  return {
    ...row,
    roleName: role?.roleName || row.roleName || '',
    roleCode: role?.roleCode || row.roleCode || '',
  }
}

async function loadAdmins() {
  loading.value = true
  try {
    const result = await pageAdminsApi(1, null)
    const data = result?.data || {}
    tableData.value = (data.records || []).map(enrichAdminRow)
    applyFilter()
  } catch (error) {
    ElMessage.error(error.message || '获取管理员列表失败')
  } finally {
    loading.value = false
  }
}

async function loadAdminRoles() {
  try {
    const rolesResp = await listRolesApi()
    const allRoles = Array.isArray(rolesResp?.data) ? rolesResp.data : []
    roleOptions.value = allRoles.filter((role) =>
      ['ROOT', 'ADMIN'].includes(String(role.roleCode || '').toUpperCase())
    )
    if (!form.roleId && roleOptions.value.length) {
      const adminRole = roleOptions.value.find((r) => String(r.roleCode).toUpperCase() === 'ADMIN')
      form.roleId = Number((adminRole || roleOptions.value[0]).roleId)
    }
  } catch (error) {
    const msg = String(error?.message || '')
    if (msg.includes('403') || msg.includes('无权限')) {
      roleOptions.value = []
      return
    }
    ElMessage.error(msg || '加载管理员角色失败')
  }
}

function openCreate() {
  isEdit.value = false
  resetForm()
  const adminRole = roleOptions.value.find((r) => String(r.roleCode).toUpperCase() === 'ADMIN')
  form.roleId = adminRole ? Number(adminRole.roleId) : null
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    account: row.account || '',
    username: row.username || '',
    phone: row.phone || '',
    email: row.email || '',
    password: '',
    sex: row.sex != null && row.sex !== '' ? String(row.sex) : '0',
    remark: row.remark || '',
    roleId: Number(row.roleId)
  })
  dialogVisible.value = true
}

async function submitForm() {
  try {
    const payload = {
      id: form.id,
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      sex: form.sex != null ? String(form.sex) : '0',
      remark: form.remark || '',
      roleId: Number(form.roleId)
    }
    if (!isEdit.value) {
      const acc = String(form.account || '').trim()
      if (!acc) {
        ElMessage.warning('请填写管理员登录账号')
        return
      }
      payload.account = acc
    }
    if (form.password) payload.password = form.password
    if (isEdit.value) {
      await updateUserApi(payload)
      ElMessage.success('编辑成功')
    } else {
      await saveUserApi(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadAdmins()
  } catch (error) {
    ElMessage.error(error.message || (isEdit.value ? '编辑失败' : '新增失败'))
  }
}

async function removeAdmin(id) {
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    await loadAdmins()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function getRoleColor(code) {
  const c = String(code || '').toUpperCase()
  if (c === 'ROOT') return 'root'
  if (c === 'ADMIN') return 'admin'
  return 'default'
}

onMounted(async () => {
  await loadAdminRoles()
  await loadAdmins()
})
</script>

<template>
  <FlatManageListView
    v-model:search-query="searchQuery"
    title="管理员列表"
    desc="管理账号、联系方式与权限角色"
    :loading="loading"
    :data="filteredData"
    :columns="columns"
    row-key="id"
    search-placeholder="搜索账号、昵称、手机..."
    empty-text="暂无管理员数据"
    @refresh="loadAdmins"
    @search="applyFilter"
  >
    <template #toolbar-actions>
      <button v-permission="'admin:add'" type="button" class="mlv-btn-primary" @click="openCreate">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        新增管理员
      </button>
    </template>

    <template #cell-account="{ row }">
      <span class="account-text">{{ row.account || '-' }}</span>
    </template>

    <template #cell-roleName="{ row }">
      <span class="role-tag" :class="`role-tag--${getRoleColor(row.roleCode)}`">
        {{ row.roleName || '-' }}
      </span>
    </template>

    <template #actions="{ row }">
      <div class="action-btns">
        <button v-permission="'admin:modify'" type="button" class="btn-action btn-action--edit" title="编辑" @click="openEdit(row)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <el-popconfirm v-permission="'admin:remove'" title="确认删除该管理员？" width="220" @confirm="removeAdmin(row.id)">
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
    :title="isEdit ? '编辑管理员' : '新增管理员'"
    width="520px"
    :confirm-text="isEdit ? '保存修改' : '确认新增'"
    @confirm="submitForm"
  >
    <el-form label-position="top" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <el-form-item v-if="!isEdit" label="登录账号" class="dialog-item">
          <el-input v-model.trim="form.account" placeholder="由创建人指定" />
        </el-form-item>
        <el-form-item v-else label="账号" class="dialog-item">
          <el-input v-model="form.account" disabled />
        </el-form-item>

        <el-form-item label="用户昵称" class="dialog-item">
          <el-input v-model.trim="form.username" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="手机号" class="dialog-item">
          <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱" class="dialog-item">
          <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="性别" class="dialog-item">
          <el-select v-model="form.sex" placeholder="请选择" style="width: 100%">
            <el-option label="男" value="0" />
            <el-option label="女" value="1" />
          </el-select>
        </el-form-item>

        <el-form-item label="角色" class="dialog-item">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.roleId"
              :label="`${role.roleName} (${role.roleCode})`"
              :value="Number(role.roleId)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="密码" class="dialog-item dialog-item--full">
          <el-input v-model.trim="form.password" show-password :placeholder="isEdit ? '留空则不修改' : '请设置登录密码'" />
        </el-form-item>

        <el-form-item label="备注" class="dialog-item dialog-item--full">
          <el-input v-model.trim="form.remark" type="textarea" :rows="2" placeholder="选填" resize="none" />
        </el-form-item>
      </div>
    </el-form>
  </DataOperationView>
</template>
