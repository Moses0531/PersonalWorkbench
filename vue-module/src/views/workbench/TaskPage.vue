<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addTaskApi,
  deleteTasksApi,
  pageTasksApi,
  updateTaskApi,
} from '@/apis/workbench/TaskApi'
import { pageProjectsApi } from '@/apis/workbench/ProjectApi'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const STATUS_COLUMNS = [
  { key: '0', label: '待办', hint: '还未动手' },
  { key: '1', label: '进行中', hint: '正在推进' },
  { key: '2', label: '已完成', hint: '可以收尾' },
  { key: '3', label: '已取消', hint: '不再跟进' },
]

const PRIORITY_OPTIONS = [
  { value: '0', label: '低' },
  { value: '1', label: '中' },
  { value: '2', label: '高' },
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tasks = ref([])
const projects = ref([])
const searchQuery = ref('')
const filterPriority = ref(null)
const filterProjectId = ref(null)

const form = reactive({
  taskId: null,
  projectId: null,
  title: '',
  description: '',
  status: '0',
  priority: '1',
  dueTime: null,
  tags: '',
  displayOrder: 0,
  remark: '',
})

const projectMap = computed(() => {
  const map = new Map()
  for (const p of projects.value) map.set(Number(p.projectId), p.name)
  return map
})

const filteredTasks = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return tasks.value.filter((t) => {
    if (filterPriority.value != null && String(t.priority) !== String(filterPriority.value)) return false
    if (filterProjectId.value != null && Number(t.projectId) !== Number(filterProjectId.value)) return false
    if (!q) return true
    return (
      (t.title || '').toLowerCase().includes(q) ||
      (t.description || '').toLowerCase().includes(q) ||
      (t.tags || '').toLowerCase().includes(q)
    )
  })
})

const columnsData = computed(() =>
  STATUS_COLUMNS.map((col) => ({
    ...col,
    items: filteredTasks.value
      .filter((t) => String(t.status ?? '0') === col.key)
      .slice()
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)),
  })),
)

const stats = computed(() => {
  const all = tasks.value
  const todo = all.filter((t) => String(t.status) === '0').length
  const doing = all.filter((t) => String(t.status) === '1').length
  const done = all.filter((t) => String(t.status) === '2').length
  const overdue = all.filter((t) => {
    if (!t.dueTime || String(t.status) === '2' || String(t.status) === '3') return false
    return dayjs(t.dueTime).isBefore(dayjs(), 'day')
  }).length
  return { total: all.length, todo, doing, done, overdue }
})

function resetForm(defaults = {}) {
  Object.assign(form, {
    taskId: null,
    projectId: null,
    title: '',
    description: '',
    status: '0',
    priority: '1',
    dueTime: null,
    tags: '',
    displayOrder: 0,
    remark: '',
    ...defaults,
  })
}

function projectNameOf(projectId) {
  if (projectId == null || projectId === '') return ''
  return projectMap.value.get(Number(projectId)) || ''
}

function priorityClass(priority) {
  const p = String(priority ?? '1')
  if (p === '2') return 'wb-chip--prio-high'
  if (p === '0') return 'wb-chip--prio-low'
  return 'wb-chip--prio-mid'
}

function priorityLabel(priority) {
  return PRIORITY_OPTIONS.find((o) => o.value === String(priority))?.label || '中'
}

function formatDue(value) {
  if (!value) return ''
  const d = dayjs(value)
  if (!d.isValid()) return ''
  return d.format('MM-DD HH:mm')
}

function isOverdue(task) {
  if (!task.dueTime || String(task.status) === '2' || String(task.status) === '3') return false
  return dayjs(task.dueTime).isBefore(dayjs(), 'day')
}

function tagList(tags) {
  if (!tags) return []
  return String(tags)
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
    .slice(0, 4)
}

async function loadProjects() {
  try {
    const result = await pageProjectsApi(1, null)
    projects.value = (result?.data?.records || []).filter((p) => String(p.status) !== '1')
  } catch {
    projects.value = []
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const result = await pageTasksApi(1, null)
    tasks.value = result?.data?.records || []
  } catch (error) {
    message.error(error.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadTasks(), loadProjects()])
}

function openCreate(status = '0') {
  isEdit.value = false
  resetForm({ status: String(status) })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  resetForm({
    taskId: row.taskId,
    projectId: row.projectId != null ? Number(row.projectId) : null,
    title: row.title || '',
    description: row.description || '',
    status: row.status != null ? String(row.status) : '0',
    priority: row.priority != null ? String(row.priority) : '1',
    dueTime: row.dueTime ? dayjs(row.dueTime) : null,
    tags: row.tags || '',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

function toPayload() {
  return {
    taskId: form.taskId,
    projectId: form.projectId != null ? Number(form.projectId) : null,
    title: form.title.trim(),
    description: form.description?.trim() || '',
    status: String(form.status ?? '0'),
    priority: String(form.priority ?? '1'),
    dueTime: form.dueTime ? dayjs(form.dueTime).format('YYYY-MM-DD HH:mm:ss') : null,
    tags: form.tags?.trim() || '',
    displayOrder: Number(form.displayOrder) || 0,
    remark: form.remark?.trim() || '',
  }
}

async function submitForm() {
  if (!form.title?.trim()) {
    message.warning('请填写任务标题')
    return
  }
  submitting.value = true
  try {
    const payload = toPayload()
    if (isEdit.value) await updateTaskApi(payload)
    else await addTaskApi(payload)
    message.success('操作成功')
    dialogVisible.value = false
    await loadTasks()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function moveTask(task, nextStatus) {
  if (String(task.status) === String(nextStatus)) return
  try {
    await updateTaskApi({ ...task, status: String(nextStatus) })
    await loadTasks()
  } catch (error) {
    message.error(error.message || '状态更新失败')
  }
}

async function removeTask(id) {
  try {
    await deleteTasksApi(id)
    message.success('删除成功')
    await loadTasks()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

onMounted(refreshAll)
</script>

<template>
  <div class="wb-page task-page">
    <div class="wb-page__blob wb-page__blob--1" aria-hidden="true" />
    <div class="wb-page__blob wb-page__blob--2" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header">
        <div class="wb-header__text">
          <h1 class="wb-header__title">任务看板</h1>
          <p class="wb-header__desc">按状态推进个人任务，支持关联项目与优先级。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="refreshAll">刷新</button>
          <button v-permission="'task:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate('0')">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新建任务
          </button>
        </div>
      </header>

      <div class="wb-stats">
        <div class="wb-stat wb-stat--accent">
          <span class="wb-stat__label">全部任务</span>
          <span class="wb-stat__value">{{ stats.total }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">待办</span>
          <span class="wb-stat__value">{{ stats.todo }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">进行中</span>
          <span class="wb-stat__value">{{ stats.doing }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">已逾期</span>
          <span class="wb-stat__value">{{ stats.overdue }}</span>
          <span class="wb-stat__hint">未完成且已过截止日</span>
        </div>
      </div>

      <div class="wb-toolbar">
        <input
          v-model="searchQuery"
          type="search"
          class="wb-search"
          placeholder="搜索标题、标签..."
          aria-label="搜索任务"
        />
        <a-select
          v-model:value="filterPriority"
          allow-clear
          placeholder="优先级"
          style="width: 120px"
          :options="PRIORITY_OPTIONS"
        />
        <a-select
          v-model:value="filterProjectId"
          allow-clear
          placeholder="所属项目"
          style="width: 180px"
          :options="projects.map((p) => ({ value: Number(p.projectId), label: p.name }))"
        />
      </div>

      <a-spin :spinning="loading">
        <div v-if="!loading && !tasks.length" class="wb-empty">
          <h2 class="wb-empty__title">看板还是空的</h2>
          <p class="wb-empty__desc">创建第一条任务，把它放进「待办」列，再逐步推进。</p>
          <button v-permission="'task:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate('0')">
            新建任务
          </button>
        </div>

        <div v-else class="kanban">
          <section v-for="col in columnsData" :key="col.key" class="kanban-col">
            <header class="kanban-col__head">
              <div class="kanban-col__title-wrap">
                <h2 class="kanban-col__title">{{ col.label }}</h2>
                <span class="kanban-col__count">{{ col.items.length }}</span>
              </div>
              <button
                v-permission="'task:add'"
                type="button"
                class="kanban-col__add"
                :title="`在${col.label}新建`"
                @click="openCreate(col.key)"
              >
                +
              </button>
            </header>

            <div class="kanban-col__body">
              <article
                v-for="task in col.items"
                :key="task.taskId"
                class="task-card"
                :class="{ 'task-card--overdue': isOverdue(task) }"
              >
                <div class="task-card__top">
                  <span class="wb-chip" :class="priorityClass(task.priority)">
                    {{ priorityLabel(task.priority) }}
                  </span>
                  <div class="task-card__ops">
                    <button
                      v-permission="'task:modify'"
                      type="button"
                      class="task-card__op"
                      title="编辑"
                      @click="openEdit(task)"
                    >
                      编辑
                    </button>
                    <a-popconfirm
                      v-permission="'task:remove'"
                      title="确认删除该任务？"
                      @confirm="removeTask(task.taskId)"
                    >
                      <button type="button" class="task-card__op task-card__op--danger">删除</button>
                    </a-popconfirm>
                  </div>
                </div>

                <h3 class="task-card__title">{{ task.title }}</h3>
                <p v-if="task.description" class="task-card__desc">{{ task.description }}</p>

                <div v-if="tagList(task.tags).length" class="task-card__tags">
                  <span v-for="tag in tagList(task.tags)" :key="tag" class="task-tag">{{ tag }}</span>
                </div>

                <footer class="task-card__meta">
                  <span v-if="projectNameOf(task.projectId)" class="task-card__project">
                    {{ projectNameOf(task.projectId) }}
                  </span>
                  <span
                    v-if="task.dueTime"
                    class="task-card__due"
                    :class="{ 'is-overdue': isOverdue(task) }"
                  >
                    {{ formatDue(task.dueTime) }}
                  </span>
                </footer>

                <div v-permission="'task:modify'" class="task-card__move">
                  <button
                    v-for="target in STATUS_COLUMNS.filter((c) => c.key !== col.key)"
                    :key="target.key"
                    type="button"
                    class="task-move-btn"
                    @click="moveTask(task, target.key)"
                  >
                    → {{ target.label }}
                  </button>
                </div>
              </article>

              <div v-if="!col.items.length" class="kanban-col__empty">暂无任务</div>
            </div>
          </section>
        </div>
      </a-spin>
    </div>

    <DataOperationView
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新建任务'"
      width="560px"
      :loading="submitting"
      :confirm-text="isEdit ? '保存修改' : '确认创建'"
      @confirm="submitForm"
    >
      <a-form layout="vertical" :model="form" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="标题" class="dialog-item dialog-item--full" required>
            <a-input v-model:value.trim="form.title" placeholder="要完成什么？" :maxlength="120" />
          </a-form-item>

          <a-form-item label="状态" class="dialog-item">
            <a-select v-model:value="form.status" style="width: 100%">
              <a-select-option v-for="col in STATUS_COLUMNS" :key="col.key" :value="col.key">
                {{ col.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="优先级" class="dialog-item">
            <a-select v-model:value="form.priority" style="width: 100%">
              <a-select-option v-for="opt in PRIORITY_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="所属项目" class="dialog-item">
            <a-select v-model:value="form.projectId" allow-clear placeholder="独立任务可不选" style="width: 100%">
              <a-select-option v-for="p in projects" :key="p.projectId" :value="Number(p.projectId)">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="截止时间" class="dialog-item">
            <a-date-picker
              v-model:value="form.dueTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
              placeholder="选填"
            />
          </a-form-item>

          <a-form-item label="标签" class="dialog-item dialog-item--full">
            <a-input v-model:value.trim="form.tags" placeholder="逗号分隔，如：设计,本周" />
          </a-form-item>

          <a-form-item label="描述" class="dialog-item dialog-item--full">
            <a-textarea v-model:value.trim="form.description" :rows="3" placeholder="补充上下文" />
          </a-form-item>

          <a-form-item label="显示顺序" class="dialog-item">
            <a-input-number v-model:value="form.displayOrder" :min="0" style="width: 100%" />
          </a-form-item>

          <a-form-item label="备注" class="dialog-item">
            <a-input v-model:value.trim="form.remark" placeholder="选填" />
          </a-form-item>
        </div>
      </a-form>
    </DataOperationView>
  </div>
</template>

<style scoped>
.kanban {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
  align-items: start;
}

.kanban-col {
  display: flex;
  flex-direction: column;
  min-height: 280px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.kanban-col__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 14px 10px;
  border-bottom: 1px solid var(--color-border-light);
  background: linear-gradient(180deg, rgba(248, 252, 255, 0.95) 0%, rgba(255, 255, 255, 0.4) 100%);
}

.kanban-col__title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.kanban-col__title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.kanban-col__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: var(--radius-full);
  font-size: 0.72rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

.kanban-col__add {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.kanban-col__add:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
}

.kanban-col__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  min-height: 160px;
}

.kanban-col__empty {
  padding: 28px 8px;
  text-align: center;
  font-size: 0.8rem;
  color: var(--color-text-dim);
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-lg);
  background: #fff;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.task-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.task-card--overdue {
  border-color: rgba(224, 85, 69, 0.28);
  background: linear-gradient(180deg, rgba(224, 85, 69, 0.04) 0%, #fff 40%);
}

.task-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-card__ops {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.task-card:hover .task-card__ops,
.task-card:focus-within .task-card__ops {
  opacity: 1;
}

.task-card__op {
  border: none;
  background: transparent;
  padding: 2px 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 4px;
}

.task-card__op:hover {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

.task-card__op--danger:hover {
  color: var(--color-red);
  background: var(--color-red-soft);
}

.task-card__title {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.35;
  color: var(--color-text-primary);
}

.task-card__desc {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.task-tag {
  padding: 2px 7px;
  border-radius: var(--radius-xs);
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--color-text-body);
  background: var(--color-surface-muted);
}

.task-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-size: 0.72rem;
  color: var(--color-text-dim);
}

.task-card__project {
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-accent-deep);
  font-weight: 600;
}

.task-card__due.is-overdue {
  color: var(--color-red);
  font-weight: 650;
}

.task-card__move {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-top: 4px;
  border-top: 1px solid var(--color-border-light);
}

.task-move-btn {
  border: none;
  background: transparent;
  padding: 2px 4px;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--color-text-dim);
  cursor: pointer;
  border-radius: 4px;
}

.task-move-btn:hover {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

@media (max-width: 1100px) {
  .kanban {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kanban {
    grid-template-columns: 1fr;
  }

  .task-card__ops {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .task-card,
  .task-card__ops {
    transition: none;
  }
}
</style>
