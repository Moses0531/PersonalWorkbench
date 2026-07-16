<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addTaskApi,
  deleteTasksApi,
  pageTasksApi,
  updateTaskApi,
} from '@/apis/workbench/TaskApi'
import {
  addProjectApi,
  deleteProjectsApi,
  pageProjectsApi,
  updateProjectApi,
} from '@/apis/workbench/ProjectApi'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const route = useRoute()
const router = useRouter()

const STATUS_COLUMNS = [
  { key: '0', label: '待办' },
  { key: '1', label: '进行中' },
  { key: '2', label: '已完成' },
  { key: '3', label: '已取消' },
]

const activeTab = ref(route.query.tab === 'projects' ? 'projects' : 'tasks')
const filterProjectId = ref(
  route.query.projectId != null && route.query.projectId !== ''
    ? Number(route.query.projectId)
    : null,
)
const syncingQuery = ref(false)

const inProjectSpace = computed(() => filterProjectId.value != null)

const currentProject = computed(() => {
  if (!inProjectSpace.value) return null
  const id = Number(filterProjectId.value)
  return projects.value.find((p) => Number(p.projectId) === id) || null
})

const filterProjectName = computed(() => {
  if (!inProjectSpace.value) return ''
  return currentProject.value?.name || projectMap.value.get(Number(filterProjectId.value)) || `项目 #${filterProjectId.value}`
})

const headerCopy = computed(() => {
  if (inProjectSpace.value) {
    const desc = (currentProject.value?.description || '').trim()
    return {
      title: filterProjectName.value,
      desc: desc || '项目空间 · 按状态推进本项目任务',
    }
  }
  if (activeTab.value === 'projects') {
    return {
      title: '事务',
      desc: '梳理进行中与已归档的个人项目，点项目名进入项目空间。',
    }
  }
  return {
    title: '事务',
    desc: '按状态推进个人任务，支持关联项目与优先级。',
  }
})

function buildQuery() {
  const query = {}
  if (activeTab.value === 'projects') {
    query.tab = 'projects'
  } else if (filterProjectId.value != null && !Number.isNaN(Number(filterProjectId.value))) {
    query.projectId = String(filterProjectId.value)
  }
  return query
}

function syncRouteQuery() {
  const next = buildQuery()
  const curTab = route.query.tab === 'projects' ? 'projects' : 'tasks'
  const curProject =
    route.query.projectId != null && route.query.projectId !== ''
      ? String(route.query.projectId)
      : undefined
  const nextTab = next.tab === 'projects' ? 'projects' : 'tasks'
  const nextProject = next.projectId

  if (curTab === nextTab && curProject === nextProject) return

  syncingQuery.value = true
  router.replace({ path: route.path, query: next }).finally(() => {
    syncingQuery.value = false
  })
}

function setTab(tab) {
  if (activeTab.value === tab && !inProjectSpace.value) return
  activeTab.value = tab
  filterProjectId.value = null
}

function viewProjectTasks(projectId) {
  filterProjectId.value = Number(projectId)
  activeTab.value = 'tasks'
}

function leaveProjectSpace() {
  filterProjectId.value = null
  activeTab.value = 'projects'
}

watch([activeTab, filterProjectId], () => {
  syncRouteQuery()
})

watch(
  () => [route.query.tab, route.query.projectId],
  () => {
    if (syncingQuery.value) return
    activeTab.value = route.query.tab === 'projects' ? 'projects' : 'tasks'
    filterProjectId.value =
      route.query.projectId != null && route.query.projectId !== ''
        ? Number(route.query.projectId)
        : null
  },
)

/* ---------- 任务列表 ---------- */
const taskLoading = ref(false)
const taskSubmitting = ref(false)
const taskDialogVisible = ref(false)
const taskIsEdit = ref(false)
const tasks = ref([])
const projects = ref([])
const taskSearchQuery = ref('')
const filterPriority = ref(null)

const taskForm = reactive({
  taskId: null,
  projectId: null,
  title: '',
  description: '',
  status: '0',
  priority: 0,
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

const activeProjects = computed(() =>
  projects.value.filter((p) => String(p.status) !== '1'),
)

const projectScopedTasks = computed(() => {
  if (!inProjectSpace.value) return tasks.value
  const id = Number(filterProjectId.value)
  return tasks.value.filter((t) => Number(t.projectId) === id)
})

const filteredTasks = computed(() => {
  const q = taskSearchQuery.value.trim().toLowerCase()
  return projectScopedTasks.value.filter((t) => {
    if (filterPriority.value != null && Number(t.priority) !== Number(filterPriority.value)) return false
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

/** 当前状态 Tab */
const statusTab = ref('0')

const activeStatusColumn = computed(
  () => columnsData.value.find((c) => c.key === statusTab.value) || columnsData.value[0],
)

const statusTabTasks = computed(() => activeStatusColumn.value?.items || [])

const taskStats = computed(() => {
  const all = projectScopedTasks.value
  const todo = all.filter((t) => String(t.status) === '0').length
  const doing = all.filter((t) => String(t.status) === '1').length
  const overdue = all.filter((t) => {
    if (!t.dueTime || String(t.status) === '2' || String(t.status) === '3') return false
    return dayjs(t.dueTime).isBefore(dayjs(), 'day')
  }).length
  return { total: all.length, todo, doing, overdue }
})

function resetTaskForm(defaults = {}) {
  Object.assign(taskForm, {
    taskId: null,
    projectId: null,
    title: '',
    description: '',
    status: '0',
    priority: 0,
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

async function loadProjectsForSelect() {
  try {
    const result = await pageProjectsApi(1, null)
    projects.value = result?.data?.records || []
  } catch {
    projects.value = []
  }
}

async function loadTasks() {
  taskLoading.value = true
  try {
    const result = await pageTasksApi(1, null)
    tasks.value = result?.data?.records || []
  } catch (error) {
    message.error(error.message || '获取任务列表失败')
  } finally {
    taskLoading.value = false
  }
}

async function refreshTasks() {
  await Promise.all([loadTasks(), loadProjectsForSelect()])
}

function openCreateTask(status = '0') {
  taskIsEdit.value = false
  resetTaskForm({
    status: String(status),
    projectId: filterProjectId.value != null ? Number(filterProjectId.value) : null,
  })
  taskDialogVisible.value = true
}

function openEditTask(row) {
  taskIsEdit.value = true
  resetTaskForm({
    taskId: row.taskId,
    projectId: row.projectId != null ? Number(row.projectId) : null,
    title: row.title || '',
    description: row.description || '',
    status: row.status != null ? String(row.status) : '0',
    priority: row.priority != null ? Number(row.priority) : 0,
    dueTime: row.dueTime ? dayjs(row.dueTime) : null,
    tags: row.tags || '',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  taskDialogVisible.value = true
}

function toTaskPayload() {
  return {
    taskId: taskForm.taskId,
    projectId: taskForm.projectId != null ? Number(taskForm.projectId) : null,
    title: taskForm.title.trim(),
    description: taskForm.description?.trim() || '',
    status: String(taskForm.status ?? '0'),
    priority: Number(taskForm.priority ?? 0),
    dueTime: taskForm.dueTime ? dayjs(taskForm.dueTime).format('YYYY-MM-DD HH:mm:ss') : null,
    tags: taskForm.tags?.trim() || '',
    displayOrder: Number(taskForm.displayOrder) || 0,
    remark: taskForm.remark?.trim() || '',
  }
}

async function submitTaskForm() {
  if (!taskForm.title?.trim()) {
    message.warning('请填写任务标题')
    return
  }
  taskSubmitting.value = true
  try {
    const payload = toTaskPayload()
    if (taskIsEdit.value) await updateTaskApi(payload)
    else await addTaskApi(payload)
    message.success('操作成功')
    taskDialogVisible.value = false
    await loadTasks()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    taskSubmitting.value = false
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

/* ---------- 项目列表 ---------- */
const projectLoading = ref(false)
const projectSubmitting = ref(false)
const projectDialogVisible = ref(false)
const projectIsEdit = ref(false)
const projectTableData = ref([])
const projectSearchQuery = ref('')
const filteredProjects = ref([])

const projectForm = reactive({
  projectId: null,
  name: '',
  description: '',
  status: '0',
  displayOrder: 0,
  remark: '',
})

const projectColumns = [
  { prop: 'name', label: '项目名称', cellClass: 'cell-name' },
  { prop: 'status', label: '状态', headerClass: 'th-status' },
  { prop: 'description', label: '描述' },
  { prop: 'displayOrder', label: '排序', headerClass: 'th-id', cellClass: 'cell-id' },
  { prop: 'updateTime', label: '更新时间', cellClass: 'cell-muted' },
  { prop: 'actions', label: '操作', type: 'actions', headerClass: 'th-action', align: 'center', width: '148px' },
]

const projectStats = computed(() => {
  const all = projectTableData.value
  const active = all.filter((r) => String(r.status) === '0').length
  const archived = all.filter((r) => String(r.status) === '1').length
  return { total: all.length, active, archived }
})

function resetProjectForm() {
  Object.assign(projectForm, {
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

function applyProjectFilter() {
  const q = projectSearchQuery.value.trim().toLowerCase()
  if (!q) {
    filteredProjects.value = projectTableData.value
    return
  }
  filteredProjects.value = projectTableData.value.filter(
    (row) =>
      (row.name || '').toLowerCase().includes(q) ||
      (row.description || '').toLowerCase().includes(q) ||
      (row.remark || '').toLowerCase().includes(q),
  )
}

async function loadProjectTable() {
  projectLoading.value = true
  try {
    const result = await pageProjectsApi(1, null)
    projectTableData.value = result?.data?.records || []
    applyProjectFilter()
  } catch (error) {
    message.error(error.message || '获取项目列表失败')
  } finally {
    projectLoading.value = false
  }
}

async function onProjectsChanged() {
  await Promise.all([loadProjectTable(), loadProjectsForSelect()])
}

function openCreateProject() {
  projectIsEdit.value = false
  resetProjectForm()
  projectDialogVisible.value = true
}

function openEditProject(row) {
  projectIsEdit.value = true
  Object.assign(projectForm, {
    projectId: row.projectId,
    name: row.name || '',
    description: row.description || '',
    status: row.status != null ? String(row.status) : '0',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  projectDialogVisible.value = true
}

async function submitProjectForm() {
  if (!projectForm.name?.trim()) {
    message.warning('请填写项目名称')
    return
  }
  projectSubmitting.value = true
  try {
    const payload = {
      projectId: projectForm.projectId,
      name: projectForm.name.trim(),
      description: projectForm.description?.trim() || '',
      status: String(projectForm.status ?? '0'),
      displayOrder: Number(projectForm.displayOrder) || 0,
      remark: projectForm.remark?.trim() || '',
    }
    if (projectIsEdit.value) await updateProjectApi(payload)
    else await addProjectApi(payload)
    message.success('操作成功')
    projectDialogVisible.value = false
    await onProjectsChanged()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    projectSubmitting.value = false
  }
}

async function removeProject(id) {
  try {
    await deleteProjectsApi(id)
    message.success('删除成功')
    await onProjectsChanged()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function toggleArchive(row) {
  const next = String(row.status) === '0' ? '1' : '0'
  try {
    await updateProjectApi({ ...row, status: next })
    message.success(next === '1' ? '已归档' : '已恢复进行中')
    await onProjectsChanged()
  } catch (error) {
    message.error(error.message || '状态更新失败')
  }
}

async function refreshCurrent() {
  if (activeTab.value === 'projects') {
    await loadProjectTable()
  } else {
    await refreshTasks()
  }
}

onMounted(async () => {
  await Promise.all([refreshTasks(), loadProjectTable()])
})
</script>

<template>
  <div class="wb-page task-page">
    <div class="wb-page__blob wb-page__blob--1" aria-hidden="true" />
    <div class="wb-page__blob wb-page__blob--2" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header" :class="{ 'wb-header--space': inProjectSpace }">
        <div class="wb-header__text">
          <button
            v-if="inProjectSpace"
            type="button"
            class="space-back"
            @click="leaveProjectSpace"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <polyline points="15 18 9 12 15 6" />
            </svg>
            返回项目
          </button>
          <h1 class="wb-header__title" :class="{ 'wb-header__title--space': inProjectSpace }">
            {{ headerCopy.title }}
          </h1>
          <p class="wb-header__desc">{{ headerCopy.desc }}</p>
        </div>
        <div class="wb-header__actions">
          <div v-if="!inProjectSpace" class="affair-tabs" role="tablist" aria-label="事务视图">
            <button
              type="button"
              class="affair-tab"
              :class="{ 'is-active': activeTab === 'tasks' }"
              role="tab"
              :aria-selected="activeTab === 'tasks'"
              @click="setTab('tasks')"
            >
              任务
            </button>
            <button
              type="button"
              class="affair-tab"
              :class="{ 'is-active': activeTab === 'projects' }"
              role="tab"
              :aria-selected="activeTab === 'projects'"
              @click="setTab('projects')"
            >
              项目
            </button>
          </div>
          <button type="button" class="wb-btn wb-btn--ghost" @click="refreshCurrent">刷新</button>
        </div>
      </header>

      <!-- 任务列表 -->
      <div v-show="activeTab === 'tasks'">
        <div class="wb-stats">
          <div class="wb-stat wb-stat--accent">
            <span class="wb-stat__label">{{ inProjectSpace ? '本项目任务' : '全部任务' }}</span>
            <span class="wb-stat__value">{{ taskStats.total }}</span>
          </div>
          <div class="wb-stat">
            <span class="wb-stat__label">待办</span>
            <span class="wb-stat__value">{{ taskStats.todo }}</span>
          </div>
          <div class="wb-stat">
            <span class="wb-stat__label">进行中</span>
            <span class="wb-stat__value">{{ taskStats.doing }}</span>
          </div>
          <div class="wb-stat">
            <span class="wb-stat__label">已逾期</span>
            <span class="wb-stat__value">{{ taskStats.overdue }}</span>
            <span class="wb-stat__hint">未完成且已过截止日</span>
          </div>
        </div>

        <div class="wb-toolbar">
          <input
            v-model="taskSearchQuery"
            type="search"
            class="wb-search"
            placeholder="搜索标题、标签..."
            aria-label="搜索任务"
          />
          <a-input-number
            v-model:value="filterPriority"
            placeholder="优先级"
            style="width: 120px"
            :precision="0"
          />
          <button
            v-permission="'task:add'"
            type="button"
            class="wb-btn wb-btn--primary"
            @click="openCreateTask(statusTab)"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新建任务
          </button>
        </div>

        <div class="status-tabs" role="tablist" aria-label="任务状态">
          <button
            v-for="col in columnsData"
            :key="col.key"
            type="button"
            class="status-tab"
            :class="{ 'is-active': statusTab === col.key }"
            role="tab"
            :aria-selected="statusTab === col.key"
            @click="statusTab = col.key"
          >
            <span class="status-tab__label">{{ col.label }}</span>
            <span class="status-tab__count">{{ col.items.length }}</span>
          </button>
        </div>

        <a-spin :spinning="taskLoading">
          <div v-if="!taskLoading && !projectScopedTasks.length" class="wb-empty">
            <h2 class="wb-empty__title">{{ inProjectSpace ? '项目空间还是空的' : '还没有任务' }}</h2>
            <p class="wb-empty__desc">
              {{ inProjectSpace ? '为本项目创建第一条任务，从「待办」开始推进。' : '创建第一条任务，放进「待办」，再逐步推进。' }}
            </p>
            <button v-permission="'task:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreateTask('0')">
              新建任务
            </button>
          </div>

          <div v-else class="task-panel">
            <div class="task-panel__head">
              <h2 class="task-panel__title">{{ activeStatusColumn.label }}</h2>
              <button
                v-permission="'task:add'"
                type="button"
                class="task-panel__add"
                :title="`在${activeStatusColumn.label}新建`"
                @click="openCreateTask(statusTab)"
              >
                + 添加
              </button>
            </div>

            <div class="task-list">
              <article
                v-for="task in statusTabTasks"
                :key="task.taskId"
                class="task-card"
                :class="{ 'task-card--overdue': isOverdue(task) }"
              >
                <div class="task-card__top">
                  <span class="wb-chip wb-chip--prio">P{{ task.priority ?? 0 }}</span>
                  <div class="action-btns">
                    <button
                      v-permission="'task:modify'"
                      type="button"
                      class="btn-action btn-action--edit"
                      title="编辑"
                      @click="openEditTask(task)"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                      </svg>
                    </button>
                    <a-popconfirm
                      v-permission="'task:remove'"
                      title="确认删除该任务？"
                      @confirm="removeTask(task.taskId)"
                    >
                      <button type="button" class="btn-action btn-action--delete" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </a-popconfirm>
                  </div>
                </div>

                <h3 class="task-card__title">{{ task.title }}</h3>
                <p v-if="task.description" class="task-card__desc">{{ task.description }}</p>

                <div v-if="tagList(task.tags).length" class="task-card__tags">
                  <span v-for="tag in tagList(task.tags)" :key="tag" class="task-tag">{{ tag }}</span>
                </div>

                <footer class="task-card__meta">
                  <span v-if="!inProjectSpace && projectNameOf(task.projectId)" class="task-card__project">
                    {{ projectNameOf(task.projectId) }}
                  </span>
                  <span
                    v-if="task.dueTime"
                    class="task-card__due"
                    :class="{ 'is-overdue': isOverdue(task) }"
                  >
                    {{ formatDue(task.dueTime) }}
                  </span>
                  <a-select
                    v-permission="'task:modify'"
                    :value="String(task.status ?? '0')"
                    size="small"
                    class="task-card__status"
                    :options="STATUS_COLUMNS.map((c) => ({ value: c.key, label: c.label }))"
                    @change="(v) => moveTask(task, v)"
                  />
                </footer>
              </article>

              <div v-if="!statusTabTasks.length" class="task-list__empty">
                <p class="task-list__empty-title">暂无「{{ activeStatusColumn.label }}」任务</p>
                <p class="task-list__empty-desc">切换其它状态查看，或在此新建一条。</p>
              </div>
            </div>
          </div>
        </a-spin>
      </div>

      <!-- 项目列表 -->
      <div v-show="activeTab === 'projects'">
        <FlatManageListView
          v-model:search-query="projectSearchQuery"
          embedded
          :show-background="false"
          title=""
          desc=""
          :loading="projectLoading"
          :data="filteredProjects"
          :columns="projectColumns"
          row-key="projectId"
          search-placeholder="搜索项目名称、描述..."
          empty-text="还没有项目，创建一个开始吧"
          @refresh="loadProjectTable"
          @search="applyProjectFilter"
        >
          <template #stats>
            <div class="wb-stats wb-stats--inline">
              <div class="wb-stat wb-stat--accent">
                <span class="wb-stat__label">全部项目</span>
                <span class="wb-stat__value">{{ projectStats.total }}</span>
              </div>
              <div class="wb-stat">
                <span class="wb-stat__label">进行中</span>
                <span class="wb-stat__value">{{ projectStats.active }}</span>
              </div>
              <div class="wb-stat">
                <span class="wb-stat__label">已归档</span>
                <span class="wb-stat__value">{{ projectStats.archived }}</span>
              </div>
            </div>
          </template>

          <template #toolbar-actions>
            <button v-permission="'project:add'" type="button" class="mlv-btn-primary" @click="openCreateProject">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              新建项目
            </button>
          </template>

          <template #cell-name="{ row }">
            <div class="proj-name-cell">
              <span class="proj-name-dot" :class="String(row.status) === '0' ? 'is-active' : 'is-archived'" />
              <button type="button" class="proj-name-link" @click="viewProjectTasks(row.projectId)">
                {{ row.name || '-' }}
              </button>
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
                type="button"
                class="btn-action btn-action--perm"
                title="查看任务"
                @click="viewProjectTasks(row.projectId)"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 11l3 3L22 4" />
                  <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
                </svg>
              </button>
              <button
                v-permission="'project:modify'"
                type="button"
                class="btn-action btn-action--edit"
                title="编辑"
                @click="openEditProject(row)"
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
      </div>
    </div>

    <DataOperationView
      v-model="taskDialogVisible"
      :title="taskIsEdit ? '编辑任务' : '新建任务'"
      :columns="2"
      :loading="taskSubmitting"
      :confirm-text="taskIsEdit ? '保存修改' : '确认创建'"
      @confirm="submitTaskForm"
    >
      <a-form layout="vertical" :model="taskForm" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="标题" class="dialog-item dialog-item--full" required>
            <a-input
              v-model:value.trim="taskForm.title"
              placeholder="要完成什么？"
              :maxlength="120"
              size="large"
            />
          </a-form-item>

          <a-form-item label="状态" class="dialog-item">
            <a-select v-model:value="taskForm.status" style="width: 100%">
              <a-select-option v-for="col in STATUS_COLUMNS" :key="col.key" :value="col.key">
                {{ col.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="优先级" class="dialog-item">
            <a-input-number v-model:value="taskForm.priority" :precision="0" style="width: 100%" />
          </a-form-item>

          <a-form-item label="所属项目" class="dialog-item">
            <a-select
              v-model:value="taskForm.projectId"
              allow-clear
              :disabled="inProjectSpace"
              placeholder="独立任务可不选"
              style="width: 100%"
            >
              <a-select-option v-for="p in activeProjects" :key="p.projectId" :value="Number(p.projectId)">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="截止时间" class="dialog-item">
            <a-date-picker
              v-model:value="taskForm.dueTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
              placeholder="选填"
            />
          </a-form-item>

          <a-form-item label="标签" class="dialog-item">
            <a-input v-model:value.trim="taskForm.tags" placeholder="逗号分隔，如：设计,本周" />
          </a-form-item>

          <a-form-item label="显示顺序" class="dialog-item">
            <a-input-number v-model:value="taskForm.displayOrder" :min="0" style="width: 100%" />
          </a-form-item>

          <a-form-item label="描述" class="dialog-item">
            <a-textarea v-model:value.trim="taskForm.description" :rows="2" placeholder="补充上下文" />
          </a-form-item>

          <a-form-item label="备注" class="dialog-item">
            <a-textarea v-model:value.trim="taskForm.remark" :rows="2" placeholder="选填" />
          </a-form-item>
        </div>
      </a-form>
    </DataOperationView>

    <DataOperationView
      v-model="projectDialogVisible"
      :title="projectIsEdit ? '编辑项目' : '新建项目'"
      :columns="2"
      :loading="projectSubmitting"
      :confirm-text="projectIsEdit ? '保存修改' : '确认创建'"
      @confirm="submitProjectForm"
    >
      <a-form layout="vertical" :model="projectForm" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="项目名称" class="dialog-item dialog-item--full" required>
            <a-input v-model:value.trim="projectForm.name" placeholder="例如：个人知识库改版" :maxlength="100" />
          </a-form-item>

          <a-form-item label="状态" class="dialog-item">
            <a-select v-model:value="projectForm.status" style="width: 100%">
              <a-select-option value="0">进行中</a-select-option>
              <a-select-option value="1">已归档</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="显示顺序" class="dialog-item">
            <a-input-number v-model:value="projectForm.displayOrder" :min="0" style="width: 100%" placeholder="越小越靠前" />
          </a-form-item>

          <a-form-item label="描述" class="dialog-item dialog-item--full">
            <a-textarea v-model:value.trim="projectForm.description" :rows="3" placeholder="一句话说明这个项目在做什么" :maxlength="500" />
          </a-form-item>

          <a-form-item label="备注" class="dialog-item dialog-item--full">
            <a-textarea v-model:value.trim="projectForm.remark" :rows="2" placeholder="选填" />
          </a-form-item>
        </div>
      </a-form>
    </DataOperationView>
  </div>
</template>

<style scoped>
.affair-tabs {
  display: inline-flex;
  padding: 3px;
  border-radius: var(--radius-control);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--color-border-light);
}

.affair-tab {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.15s ease;
}

.affair-tab:hover {
  color: var(--color-text-primary);
}

.affair-tab:active {
  transform: scale(0.98);
}

.affair-tab.is-active {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

.space-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0 0 8px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-accent-deep);
  cursor: pointer;
  transition: color 0.15s ease, transform 0.15s ease;
}

.space-back:hover {
  color: var(--color-accent);
}

.space-back:active {
  transform: translateX(-2px);
}

.wb-header__title--space {
  font-size: clamp(1.75rem, 1.35rem + 1.4vw, 2.35rem);
  letter-spacing: -0.04em;
  line-height: 1.15;
}

.wb-header--space .wb-header__desc {
  max-width: 52ch;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 0 0 var(--space-4);
  padding: 4px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.15s ease;
}

.status-tab:hover {
  color: var(--color-text-primary);
  background: rgba(255, 255, 255, 0.7);
}

.status-tab:active {
  transform: scale(0.98);
}

.status-tab:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.status-tab.is-active {
  color: var(--color-accent-deep);
  background: #fff;
  box-shadow: var(--shadow-xs);
}

.status-tab__count {
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
  color: var(--color-text-dim);
  background: var(--color-surface-muted);
}

.status-tab.is-active .status-tab__count {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

.task-panel {
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.task-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light);
  background: linear-gradient(180deg, rgba(248, 252, 255, 0.95) 0%, rgba(255, 255, 255, 0.4) 100%);
}

.task-panel__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.task-panel__add {
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
  font-size: 0.8125rem;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.15s ease;
}

.task-panel__add:hover {
  filter: brightness(0.97);
}

.task-panel__add:active {
  transform: scale(0.98);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px 18px;
  min-height: 200px;
}

.task-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 48px 16px;
  text-align: center;
}

.task-list__empty-title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 650;
  color: var(--color-text-secondary);
}

.task-list__empty-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-dim);
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: #fff;
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
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

.task-card .action-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

.task-card .btn-action {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.task-card .btn-action:active:not(:disabled) {
  transform: scale(0.96);
}

.task-card .btn-action:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.task-card .btn-action--edit {
  color: var(--color-text-secondary);
}

.task-card .btn-action--edit:hover {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}

.task-card .btn-action--delete {
  color: var(--color-text-dim);
}

.task-card .btn-action--delete:hover {
  color: var(--color-red);
  background: var(--color-red-soft);
  border-color: rgba(224, 85, 69, 0.35);
}

.task-card__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  line-height: 1.4;
  color: var(--color-text-primary);
  text-wrap: pretty;
}

.task-card__desc {
  margin: 0;
  max-width: 65ch;
  font-size: 0.8125rem;
  line-height: 1.5;
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
  gap: 10px;
  font-size: 0.72rem;
  color: var(--color-text-dim);
}

.task-card__project {
  max-width: 240px;
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

.task-card__status {
  width: 108px;
  margin-left: auto;
}

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

.proj-name-link {
  border: none;
  background: transparent;
  padding: 0;
  font: inherit;
  font-weight: 650;
  color: var(--color-text-primary);
  cursor: pointer;
  text-align: left;
}

.proj-name-link:hover {
  color: var(--color-accent-deep);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.proj-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 320px;
}

@media (max-width: 720px) {
  .status-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .status-tab {
    flex: 0 0 auto;
  }

  .task-card__status {
    margin-left: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .affair-tab,
  .status-tab,
  .space-back,
  .task-card,
  .task-panel__add {
    transition: none;
  }
}
</style>
