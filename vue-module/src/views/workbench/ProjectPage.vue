<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  addProjectApi,
  deleteProjectsApi,
  pageProjectsApi,
  updateProjectApi,
} from '@/apis/workbench/ProjectApi'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const searchQuery = ref('')
const filteredData = ref([])

const form = reactive({
  projectId: null,
  name: '',
  description: '',
  status: '0',
  displayOrder: 0,
  remark: '',
})

const columns = [
  { prop: 'name', label: '项目名称', cellClass: 'cell-name' },
  { prop: 'status', label: '状态', headerClass: 'th-status' },
  { prop: 'description', label: '描述' },
  { prop: 'displayOrder', label: '排序', headerClass: 'th-id', cellClass: 'cell-id' },
  { prop: 'updateTime', label: '更新时间', cellClass: 'cell-muted' },
  { prop: 'actions', label: '操作', type: 'actions', headerClass: 'th-action', align: 'center' },
]

const stats = computed(() => {
  const all = tableData.value
  const active = all.filter((r) => String(r.status) === '0').length
  const archived = all.filter((r) => String(r.status) === '1').length
  return {
    total: all.length,
    active,
    archived,
    ratio: all.length ? Math.round((active / all.length) * 100) : 0,
  }
})

function resetForm() {
  Object.assign(form, {
    projectId: null,
    name: '',
    description: '',
    status: '0',
    displayOrder: 0,
    remark: '',
  })
}

function formatTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

function applyFilter() {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) {
    filteredData.value = tableData.value
    return
  }
  filteredData.value = tableData.value.filter(
    (row) =>
      (row.name || '').toLowerCase().includes(q) ||
      (row.description || '').toLowerCase().includes(q) ||
      (row.remark || '').toLowerCase().includes(q),
  )
}

async function loadProjects() {
  loading.value = true
  try {
    const result = await pageProjectsApi(1, null)
    tableData.value = result?.data?.records || []
    applyFilter()
  } catch (error) {
    message.error(error.message || '获取项目列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    projectId: row.projectId,
    name: row.name || '',
    description: row.description || '',
    status: row.status != null ? String(row.status) : '0',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.name?.trim()) {
    message.warning('请填写项目名称')
    return
  }
  submitting.value = true
  try {
    const payload = {
      projectId: form.projectId,
      name: form.name.trim(),
      description: form.description?.trim() || '',
      status: String(form.status ?? '0'),
      displayOrder: Number(form.displayOrder) || 0,
      remark: form.remark?.trim() || '',
    }
    if (isEdit.value) await updateProjectApi(payload)
    else await addProjectApi(payload)
    message.success('操作成功')
    dialogVisible.value = false
    await loadProjects()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function removeProject(id) {
  try {
    await deleteProjectsApi(id)
    message.success('删除成功')
    await loadProjects()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function toggleArchive(row) {
  const next = String(row.status) === '0' ? '1' : '0'
  try {
    await updateProjectApi({
      ...row,
      status: next,
    })
    message.success(next === '1' ? '已归档' : '已恢复进行中')
    await loadProjects()
  } catch (error) {
    message.error(error.message || '状态更新失败')
  }
}

onMounted(loadProjects)
</script>

<template>
  <FlatManageListView
    v-model:search-query="searchQuery"
    title="项目管理"
    desc="梳理进行中与已归档的个人项目"
    :loading="loading"
    :data="filteredData"
    :columns="columns"
    row-key="projectId"
    search-placeholder="搜索项目名称、描述..."
    empty-text="还没有项目，创建一个开始吧"
    @refresh="loadProjects"
    @search="applyFilter"
  >
    <template #stats>
      <div class="wb-stats wb-stats--inline">
        <div class="wb-stat wb-stat--accent">
          <span class="wb-stat__label">全部项目</span>
          <span class="wb-stat__value">{{ stats.total }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">进行中</span>
          <span class="wb-stat__value">{{ stats.active }}</span>
          <span class="wb-stat__hint">可继续推进</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">已归档</span>
          <span class="wb-stat__value">{{ stats.archived }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">活跃占比</span>
          <span class="wb-stat__value">{{ stats.ratio }}%</span>
        </div>
      </div>
    </template>

    <template #toolbar-actions>
      <button v-permission="'project:add'" type="button" class="mlv-btn-primary" @click="openCreate">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        新建项目
      </button>
    </template>

    <template #cell-name="{ row }">
      <div class="proj-name-cell">
        <span class="proj-name-dot" :class="String(row.status) === '0' ? 'is-active' : 'is-archived'" />
        <span class="cell-name">{{ row.name || '-' }}</span>
      </div>
    </template>

    <template #cell-status="{ row }">
      <span
        class="wb-chip"
        :class="String(row.status) === '0' ? 'wb-chip--active' : 'wb-chip--archived'"
      >
        {{ String(row.status) === '0' ? '进行中' : '已归档' }}
      </span>
    </template>

    <template #cell-description="{ row }">
      <span class="cell-muted proj-desc">{{ row.description || '-' }}</span>
    </template>

    <template #cell-updateTime="{ row }">
      <span class="cell-muted">{{ formatTime(row.updateTime) }}</span>
    </template>

    <template #actions="{ row }">
      <div class="action-btns">
        <button
          v-permission="'project:modify'"
          type="button"
          class="btn-action btn-action--edit"
          title="编辑"
          @click="openEdit(row)"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
        </button>
        <button
          v-permission="'project:modify'"
          type="button"
          class="btn-action btn-action--perm"
          :title="String(row.status) === '0' ? '归档' : '恢复'"
          @click="toggleArchive(row)"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="21 8 21 21 3 21 3 8" />
            <rect x="1" y="3" width="22" height="5" />
            <line x1="10" y1="12" x2="14" y2="12" />
          </svg>
        </button>
        <a-popconfirm v-permission="'project:remove'" title="确认删除该项目？" @confirm="removeProject(row.projectId)">
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
    :title="isEdit ? '编辑项目' : '新建项目'"
    width="520px"
    :loading="submitting"
    :confirm-text="isEdit ? '保存修改' : '确认创建'"
    @confirm="submitForm"
  >
    <a-form layout="vertical" :model="form" class="dialog-form">
      <div class="dialog-grid">
        <a-form-item label="项目名称" class="dialog-item dialog-item--full" required>
          <a-input v-model:value.trim="form.name" placeholder="例如：个人知识库改版" :maxlength="100" />
        </a-form-item>

        <a-form-item label="状态" class="dialog-item">
          <a-select v-model:value="form.status" style="width: 100%">
            <a-select-option value="0">进行中</a-select-option>
            <a-select-option value="1">已归档</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="显示顺序" class="dialog-item">
          <a-input-number v-model:value="form.displayOrder" :min="0" style="width: 100%" placeholder="越小越靠前" />
        </a-form-item>

        <a-form-item label="描述" class="dialog-item dialog-item--full">
          <a-textarea v-model:value.trim="form.description" :rows="3" placeholder="一句话说明这个项目在做什么" :maxlength="500" />
        </a-form-item>

        <a-form-item label="备注" class="dialog-item dialog-item--full">
          <a-textarea v-model:value.trim="form.remark" :rows="2" placeholder="选填" />
        </a-form-item>
      </div>
    </a-form>
  </DataOperationView>
</template>

<style scoped>
.wb-stats--inline {
  margin-bottom: var(--space-4);
}

.proj-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.proj-name-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-text-dim);
}

.proj-name-dot.is-active {
  background: var(--color-accent);
  box-shadow: 0 0 0 3px var(--color-accent-soft);
}

.proj-name-dot.is-archived {
  background: var(--color-text-muted);
}

.proj-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 320px;
}
</style>
