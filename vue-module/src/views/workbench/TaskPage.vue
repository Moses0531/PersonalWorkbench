<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addTaskApi,
  deleteTasksApi,
  listProjectAttachmentsApi,
  pageTasksApi,
  planApplyApi,
  planPreviewApi,
  planRevokeApi,
  removeTaskAttachmentApi,
  updateTaskApi,
  uploadTaskAttachmentApi,
} from '@/apis/workbench/TaskApi'
import {
  addProjectApi,
  deleteProjectsApi,
  pageProjectsApi,
  updateProjectApi,
} from '@/apis/workbench/ProjectApi'
import FlatManageListView from '@/components/ListView/FlatManageListView.vue'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import EditorView from '@/components/EditorView.vue'
import { openKkFileViewPreview } from '@/utils/kkFileView'
import {
  LeftOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  PaperClipOutlined,
  ClockCircleOutlined,
  CheckSquareOutlined,
  InboxOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

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

const projectAttachmentsReadonly = computed(
  () => inProjectSpace.value && String(currentProject.value?.status) === '1',
)

const headerCopy = computed(() => {
  if (inProjectSpace.value) {
    const archived = projectAttachmentsReadonly.value
    const desc = (currentProject.value?.description || '').trim()
    return {
      title: filterProjectName.value,
      desc:
        desc ||
        (archived
          ? '项目已归档 · 附件只读，可下载存档'
          : '项目空间 · 按状态推进本项目任务，可审查附件'),
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

function parseAttachments(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const list = JSON.parse(raw)
      return Array.isArray(list) ? list : []
    } catch {
      return []
    }
  }
  return []
}

function attachmentCount(task) {
  return parseAttachments(task?.attachments).length
}

function formatFileSize(size) {
  const n = Number(size)
  if (!n || n < 0) return ''
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

function isTaskAttachmentsReadonly(projectId) {
  if (projectId == null || projectId === '') return false
  const p = projects.value.find((x) => Number(x.projectId) === Number(projectId))
  return p != null && String(p.status) === '1'
}

/* ---------- 任务附件 ---------- */
const taskAttachments = ref([])

const taskFormAttachmentsReadonly = computed(() =>
  isTaskAttachmentsReadonly(taskForm.projectId),
)

const projectAttachmentList = ref([])
const projectAttachmentLoading = ref(false)

async function loadProjectAttachments() {
  if (!inProjectSpace.value) {
    projectAttachmentList.value = []
    return
  }
  projectAttachmentLoading.value = true
  try {
    const result = await listProjectAttachmentsApi(filterProjectId.value)
    projectAttachmentList.value = result?.data || []
  } catch {
    projectAttachmentList.value = []
  } finally {
    projectAttachmentLoading.value = false
  }
}

watch(
  () => [inProjectSpace.value, filterProjectId.value, tasks.value],
  () => {
    if (inProjectSpace.value) loadProjectAttachments()
    else projectAttachmentList.value = []
  },
)

function openAttachmentUrl(url) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

function previewAttachment(file) {
  if (!file?.url) {
    message.warning('文件地址无效')
    return
  }
  openKkFileViewPreview(file.url)
}

function downloadAttachment(file) {
  openAttachmentUrl(file?.url)
}

/* ---------- 卡片附件弹窗 ---------- */
const filesModalVisible = ref(false)
const filesModalTask = ref(null)

const filesModalTitle = computed(() => {
  const title = (filesModalTask.value?.title || '').trim()
  return title ? `附件 · ${title}` : '附件'
})

const filesModalFiles = computed(() => parseAttachments(filesModalTask.value?.attachments))

const filesModalReadonly = computed(() =>
  isTaskAttachmentsReadonly(filesModalTask.value?.projectId),
)

function openFilesModal(task) {
  filesModalTask.value = task
  filesModalVisible.value = true
}

function closeFilesModal() {
  filesModalVisible.value = false
  filesModalTask.value = null
}

function removeFilesModalAttachment(attachmentId) {
  const task = filesModalTask.value
  if (!task) return
  return removeAttachment(attachmentId, {
    taskId: task.taskId,
    projectId: task.projectId,
  })
}

function syncAttachmentViews(taskId) {
  const row = tasks.value.find((t) => Number(t.taskId) === Number(taskId))
  if (Number(taskForm.taskId) === Number(taskId)) {
    taskAttachments.value = parseAttachments(row?.attachments)
  }
  if (filesModalTask.value && Number(filesModalTask.value.taskId) === Number(taskId)) {
    const list = parseAttachments(row?.attachments)
    if (!row || !list.length) {
      closeFilesModal()
    } else {
      filesModalTask.value = row
    }
  }
}

/** 交给 EditorView 的 uploadHandler，仍走任务附件接口 */
async function handleAttachmentUpload(file) {
  if (!file || !taskForm.taskId) {
    throw new Error('未选择有效文件')
  }
  await uploadTaskAttachmentApi(taskForm.taskId, file)
  message.success('附件已上传')
  await loadTasks()
  syncAttachmentViews(taskForm.taskId)
  if (inProjectSpace.value) await loadProjectAttachments()
  return { url: '' }
}

async function removeAttachment(attachmentId, ctx = {}) {
  const taskId = ctx.taskId ?? taskForm.taskId
  const projectId = ctx.projectId ?? taskForm.projectId
  if (!taskId || isTaskAttachmentsReadonly(projectId)) return
  try {
    await removeTaskAttachmentApi(taskId, attachmentId)
    message.success('已删除附件')
    await loadTasks()
    syncAttachmentViews(taskId)
    if (inProjectSpace.value) await loadProjectAttachments()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
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

const PLAN_BATCH_KEY = 'wb_ai_plan_batch'

const planDialogVisible = ref(false)
const planPreviewing = ref(false)
const planApplying = ref(false)
const planRevoking = ref(false)
const planForm = reactive({
  goal: '',
  deadline: null,
  constraints: '',
})

const planDeadlineHint = computed(() => {
  if (!planForm.deadline) return ''
  const days = dayjs(planForm.deadline).startOf('day').diff(dayjs().startOf('day'), 'day')
  if (days < 0) return '截止日已过'
  if (days <= 14) return `剩 ${days} 天 · 按两周冲刺极简拆解`
  if (days <= 45) return `剩 ${days} 天 · 按一到一个半月均衡拆解`
  return `剩 ${days} 天 · 按较长周期细分阶段`
})
const planPreview = ref(null)
const lastPlanBatchId = ref('')

function planBatchStorageKey(projectId) {
  return `${PLAN_BATCH_KEY}:${projectId}`
}

function loadLastPlanBatchId() {
  if (!inProjectSpace.value) {
    lastPlanBatchId.value = ''
    return
  }
  try {
    lastPlanBatchId.value = localStorage.getItem(planBatchStorageKey(filterProjectId.value)) || ''
  } catch {
    lastPlanBatchId.value = ''
  }
}

function rememberPlanBatchId(batchId) {
  lastPlanBatchId.value = batchId || ''
  if (!inProjectSpace.value || !batchId) return
  try {
    localStorage.setItem(planBatchStorageKey(filterProjectId.value), batchId)
  } catch {
    /* ignore */
  }
}

function clearRememberedPlanBatchId() {
  lastPlanBatchId.value = ''
  if (!inProjectSpace.value) return
  try {
    localStorage.removeItem(planBatchStorageKey(filterProjectId.value))
  } catch {
    /* ignore */
  }
}

watch(
  () => filterProjectId.value,
  () => loadLastPlanBatchId(),
  { immediate: true },
)

function openPlanDialog() {
  if (!inProjectSpace.value || projectAttachmentsReadonly.value) return
  planForm.goal = (currentProject.value?.description || '').trim()
  planForm.deadline = dayjs().add(14, 'day')
  planForm.constraints = ''
  planPreview.value = null
  planDialogVisible.value = true
}

async function runPlanPreview() {
  if (!filterProjectId.value) return
  if (!planForm.deadline) {
    message.warning('请先选择项目截止时间')
    return
  }
  if (dayjs(planForm.deadline).isBefore(dayjs(), 'day')) {
    message.warning('项目截止时间不能早于今天')
    return
  }
  planPreviewing.value = true
  try {
    const result = await planPreviewApi({
      projectId: Number(filterProjectId.value),
      goal: planForm.goal?.trim() || undefined,
      deadline: dayjs(planForm.deadline).format('YYYY-MM-DD'),
      constraints: planForm.constraints?.trim() || undefined,
    })
    planPreview.value = result?.data || null
    if (!planPreview.value?.phases?.length) {
      message.warning('未生成有效阶段，请补充目标后重试')
    }
  } catch (error) {
    message.error(error.message || 'AI 规划失败')
  } finally {
    planPreviewing.value = false
  }
}

async function applyPlanPreview() {
  if (!planPreview.value?.planBatchId || !planPreview.value?.phases?.length) {
    return Promise.reject(new Error('请先生成预览'))
  }
  planApplying.value = true
  try {
    const result = await planApplyApi({
      projectId: Number(filterProjectId.value),
      planBatchId: planPreview.value.planBatchId,
      phases: planPreview.value.phases,
    })
    const created = result?.data?.created ?? 0
    rememberPlanBatchId(planPreview.value.planBatchId)
    message.success(`已落板 ${created} 条任务`)
    planDialogVisible.value = false
    planPreview.value = null
    await refreshTasks()
  } catch (error) {
    message.error(error.message || '落板失败')
    return Promise.reject(error)
  } finally {
    planApplying.value = false
  }
}

async function revokeLastPlan() {
  if (!lastPlanBatchId.value) return
  planRevoking.value = true
  try {
    const result = await planRevokeApi(lastPlanBatchId.value)
    const removed = result?.data?.removed ?? 0
    clearRememberedPlanBatchId()
    message.success(removed > 0 ? `已撤销 ${removed} 条规划任务` : '没有可撤销的任务')
    await refreshTasks()
  } catch (error) {
    message.error(error.message || '撤销失败')
  } finally {
    planRevoking.value = false
  }
}

function openCreateTask(status = '0') {
  taskIsEdit.value = false
  taskAttachments.value = []
  resetTaskForm({
    status: String(status),
    projectId: filterProjectId.value != null ? Number(filterProjectId.value) : null,
  })
  taskDialogVisible.value = true
}

function openEditTask(row) {
  taskIsEdit.value = true
  taskAttachments.value = parseAttachments(row.attachments)
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
    <div class="task-page__grain" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header" :class="{ 'wb-header--space': inProjectSpace }">
        <div class="wb-header__text">
          <button
            v-if="inProjectSpace"
            type="button"
            class="space-back"
            @click="leaveProjectSpace"
          >
            <LeftOutlined />
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
      <div v-show="activeTab === 'tasks'" class="task-view">
        <div class="tp-pulse" aria-label="任务概览">
          <div class="tp-pulse__hero">
            <span class="tp-pulse__value">{{ taskStats.total }}</span>
            <span class="tp-pulse__label">{{ inProjectSpace ? '本项目任务' : '全部任务' }}</span>
          </div>
          <div class="tp-pulse__rail" role="group" aria-label="快捷筛选">
            <button
              type="button"
              class="tp-pulse__item"
              :class="{ 'is-on': statusTab === '0' }"
              @click="statusTab = '0'"
            >
              <span class="tp-pulse__item-val">{{ taskStats.todo }}</span>
              <span class="tp-pulse__item-lab">待办</span>
            </button>
            <button
              type="button"
              class="tp-pulse__item"
              :class="{ 'is-on': statusTab === '1' }"
              @click="statusTab = '1'"
            >
              <span class="tp-pulse__item-val">{{ taskStats.doing }}</span>
              <span class="tp-pulse__item-lab">进行中</span>
            </button>
            <button
              type="button"
              class="tp-pulse__item tp-pulse__item--warn"
              :class="{ 'is-on': taskStats.overdue > 0 }"
              title="未完成且已过截止日"
            >
              <span class="tp-pulse__item-val">{{ taskStats.overdue }}</span>
              <span class="tp-pulse__item-lab">已逾期</span>
            </button>
          </div>
        </div>

        <div class="wb-toolbar task-view__toolbar">
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
            class="task-view__prio"
            :precision="0"
          />
          <button
            v-if="inProjectSpace && lastPlanBatchId"
            v-permission="'task:remove'"
            type="button"
            class="wb-btn wb-btn--ghost"
            :disabled="planRevoking || projectAttachmentsReadonly"
            @click="revokeLastPlan"
          >
            撤销上次规划
          </button>
          <button
            v-if="inProjectSpace"
            v-permission="'task:add'"
            type="button"
            class="wb-btn wb-btn--ghost"
            :disabled="projectAttachmentsReadonly"
            @click="openPlanDialog"
          >
            <ThunderboltOutlined />
            智能规划
          </button>
          <button
            v-permission="'task:add'"
            type="button"
            class="wb-btn wb-btn--primary"
            @click="openCreateTask(statusTab)"
          >
            <PlusOutlined />
            新建任务
          </button>
        </div>

        <section v-if="inProjectSpace" class="attach-review" aria-label="项目附件审查">
          <div class="attach-review__head">
            <h2 class="attach-review__title">附件审查</h2>
            <span class="attach-review__meta">
              {{ projectAttachmentList.length }} 个文件
              <template v-if="projectAttachmentsReadonly"> · 已归档只读</template>
            </span>
          </div>
          <a-spin :spinning="projectAttachmentLoading">
            <ul v-if="projectAttachmentList.length" class="attach-review__list">
              <li v-for="file in projectAttachmentList" :key="`${file.taskId}-${file.id}`" class="attach-review__item">
                <div class="attach-review__info">
                  <button type="button" class="attach-review__name" :title="file.name" @click="previewAttachment(file)">
                    {{ file.name }}
                  </button>
                  <span class="attach-review__sub">
                    {{ file.taskTitle || `任务 #${file.taskId}` }}
                    <template v-if="formatFileSize(file.size)"> · {{ formatFileSize(file.size) }}</template>
                    <template v-if="file.createTime"> · {{ file.createTime }}</template>
                  </span>
                </div>
                <div class="attach-review__actions">
                  <button type="button" class="attach-review__act" @click="previewAttachment(file)">预览</button>
                  <button type="button" class="attach-review__act" @click="downloadAttachment(file)">下载</button>
                </div>
              </li>
            </ul>
            <p v-else class="attach-review__empty">本项目任务下还没有附件</p>
          </a-spin>
        </section>

        <div class="status-tabs" role="tablist" aria-label="任务状态">
          <button
            v-for="col in columnsData"
            :key="col.key"
            type="button"
            class="status-tab"
            :class="[`status-tab--s${col.key}`, { 'is-active': statusTab === col.key }]"
            role="tab"
            :aria-selected="statusTab === col.key"
            @click="statusTab = col.key"
          >
            <span class="status-tab__dot" aria-hidden="true" />
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
              <div class="task-panel__heading">
                <h2 class="task-panel__title">{{ activeStatusColumn.label }}</h2>
                <span class="task-panel__count">{{ statusTabTasks.length }}</span>
              </div>
              <button
                v-permission="'task:add'"
                type="button"
                class="task-panel__add"
                :title="`在${activeStatusColumn.label}新建`"
                @click="openCreateTask(statusTab)"
              >
                添加
              </button>
            </div>

            <div class="task-list" role="list">
              <article
                v-for="(task, idx) in statusTabTasks"
                :key="task.taskId"
                class="task-card"
                :class="[
                  `task-card--s${String(task.status ?? '0')}`,
                  { 'task-card--overdue': isOverdue(task) },
                ]"
                :style="{ '--card-i': idx }"
                role="listitem"
              >
                <div class="task-card__main">
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
                        <EditOutlined />
                      </button>
                      <a-popconfirm
                        v-permission="'task:remove'"
                        title="确认删除该任务？"
                        @confirm="removeTask(task.taskId)"
                      >
                        <button type="button" class="btn-action btn-action--delete" title="删除">
                          <DeleteOutlined />
                        </button>
                      </a-popconfirm>
                    </div>
                  </div>

                  <h3 class="task-card__title">{{ task.title }}</h3>
                  <p v-if="task.description" class="task-card__desc">{{ task.description }}</p>

                  <div v-if="tagList(task.tags).length" class="task-card__tags">
                    <span v-for="tag in tagList(task.tags)" :key="tag" class="task-tag">{{ tag }}</span>
                  </div>
                </div>

                <footer
                  v-if="(!inProjectSpace && projectNameOf(task.projectId)) || attachmentCount(task) || task.dueTime"
                  class="task-card__meta"
                >
                  <span v-if="!inProjectSpace && projectNameOf(task.projectId)" class="meta-chip meta-chip--project">
                    <FolderOutlined />
                    {{ projectNameOf(task.projectId) }}
                  </span>
                  <button
                    v-if="attachmentCount(task)"
                    type="button"
                    class="meta-chip meta-chip--files"
                    title="查看附件"
                    @click="openFilesModal(task)"
                  >
                    <PaperClipOutlined />
                    {{ attachmentCount(task) }} 附件
                  </button>
                  <span
                    v-if="task.dueTime"
                    class="meta-chip meta-chip--due"
                    :class="{ 'is-overdue': isOverdue(task) }"
                  >
                    <ClockCircleOutlined />
                    {{ formatDue(task.dueTime) }}
                  </span>
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
              <PlusOutlined />
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
                <CheckSquareOutlined />
              </button>
              <button
                v-permission="'project:modify'"
                type="button"
                class="btn-action btn-action--edit"
                title="编辑"
                @click="openEditProject(row)"
              >
                <EditOutlined />
              </button>
              <button
                v-permission="'project:modify'"
                type="button"
                class="btn-action btn-action--perm"
                :title="String(row.status) === '0' ? '归档' : '恢复'"
                @click="toggleArchive(row)"
              >
                <InboxOutlined />
              </button>
              <a-popconfirm v-permission="'project:remove'" title="确认删除该项目？" @confirm="removeProject(row.projectId)">
                <button type="button" class="btn-action btn-action--delete" title="删除">
                  <DeleteOutlined />
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

          <a-form-item label="附件" class="dialog-item dialog-item--full">
            <div class="task-attach">
              <p v-if="!taskIsEdit" class="task-attach__hint">先创建任务，再上传附件</p>
              <template v-else>
                <p v-if="taskFormAttachmentsReadonly" class="task-attach__hint">
                  所属项目已归档，附件只读，可下载
                </p>
                <ul v-if="taskAttachments.length" class="task-attach__list">
                  <li v-for="file in taskAttachments" :key="file.id" class="task-attach__item">
                    <button type="button" class="task-attach__name" :title="file.name" @click="previewAttachment(file)">
                      {{ file.name }}
                    </button>
                    <span class="task-attach__size">{{ formatFileSize(file.size) }}</span>
                    <button type="button" class="task-attach__dl" @click="previewAttachment(file)">预览</button>
                    <button type="button" class="task-attach__dl" @click="downloadAttachment(file)">下载</button>
                    <a-popconfirm
                      v-if="!taskFormAttachmentsReadonly"
                      title="确认删除该附件？"
                      @confirm="removeAttachment(file.id)"
                    >
                      <button v-permission="'task:modify'" type="button" class="task-attach__del">移除</button>
                    </a-popconfirm>
                  </li>
                </ul>
                <p v-else class="task-attach__hint">暂无附件</p>
                <div
                  v-if="!taskFormAttachmentsReadonly"
                  v-permission="'task:modify'"
                  class="task-attach__actions"
                >
                  <EditorView
                    upload-only
                    upload-text="上传附件"
                    :upload-handler="handleAttachmentUpload"
                  />
                </div>
              </template>
            </div>
          </a-form-item>
        </div>
      </a-form>
    </DataOperationView>

    <DataOperationView
      v-model="filesModalVisible"
      :title="filesModalTitle"
      :width="560"
      :columns="1"
      close-on-click-modal
      destroy-on-close
      @cancel="closeFilesModal"
    >
      <p v-if="filesModalReadonly" class="task-attach__hint files-modal__hint">
        所属项目已归档，附件只读，可预览与下载
      </p>
      <ul v-if="filesModalFiles.length" class="files-modal__list">
        <li
          v-for="file in filesModalFiles"
          :key="file.id || file.url"
          class="files-modal__item"
        >
          <div class="files-modal__info">
            <button
              type="button"
              class="files-modal__name"
              :title="file.name"
              @click="previewAttachment(file)"
            >
              {{ file.name || '未命名文件' }}
            </button>
            <span class="files-modal__meta">
              <template v-if="formatFileSize(file.size)">{{ formatFileSize(file.size) }}</template>
              <template v-if="file.createTime">
                <template v-if="formatFileSize(file.size)"> · </template>{{ file.createTime }}
              </template>
            </span>
          </div>
          <div class="files-modal__actions">
            <button type="button" class="files-modal__act" @click="previewAttachment(file)">预览</button>
            <button type="button" class="files-modal__act" @click="downloadAttachment(file)">下载</button>
            <a-popconfirm
              v-if="!filesModalReadonly"
              title="确认移除该附件？"
              @confirm="removeFilesModalAttachment(file.id)"
            >
              <button v-permission="'task:modify'" type="button" class="files-modal__act files-modal__act--danger">
                移除
              </button>
            </a-popconfirm>
          </div>
        </li>
      </ul>
      <p v-else class="task-attach__hint">暂无附件</p>
      <template #footer>
        <div class="dialog-footer dop-footer">
          <button type="button" class="btn-ghost-sm" @click="closeFilesModal">关闭</button>
        </div>
      </template>
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

    <a-modal
      v-model:open="planDialogVisible"
      title="AI 智能规划"
      width="640px"
      :mask-closable="false"
      :confirm-loading="planApplying"
      :ok-button-props="{ disabled: !planPreview?.phases?.length || planPreviewing }"
      ok-text="确认落板"
      cancel-text="关闭"
      @ok="applyPlanPreview"
    >
      <a-spin :spinning="planPreviewing">
        <div class="plan-dialog">
          <p class="plan-dialog__hint">
            必须指定整项目截止时间：两周与两个月会按不同节奏拆阶段；预览确认后再写入看板。
          </p>
          <a-form layout="vertical" class="dialog-form">
            <a-form-item label="项目截止时间" required>
              <a-date-picker
                v-model:value="planForm.deadline"
                format="YYYY-MM-DD"
                style="width: 100%"
                placeholder="整个项目要在哪天完成"
                :disabled-date="(current) => current && current < dayjs().startOf('day')"
              />
              <p v-if="planDeadlineHint" class="plan-dialog__deadline-hint">{{ planDeadlineHint }}</p>
            </a-form-item>
            <a-form-item label="规划目标">
              <a-textarea
                v-model:value="planForm.goal"
                :rows="3"
                :maxlength="2000"
                show-count
                placeholder="要达成什么？默认使用项目描述"
              />
            </a-form-item>
            <a-form-item label="其它约束（可选）">
              <a-textarea
                v-model:value="planForm.constraints"
                :rows="2"
                :maxlength="1000"
                show-count
                placeholder="如人数、技术栈、必须包含的交付物等"
              />
            </a-form-item>
          </a-form>
          <div class="plan-dialog__actions">
            <button
              type="button"
              class="wb-btn wb-btn--primary"
              :disabled="planPreviewing || !planForm.deadline"
              @click="runPlanPreview"
            >
              <ThunderboltOutlined />
              {{ planPreview ? '重新生成' : '生成预览' }}
            </button>
          </div>

          <div v-if="planPreview" class="plan-preview">
            <p v-if="planPreview.daysLeft != null" class="plan-preview__meta">
              截止 {{ planPreview.deadline }} · 剩 {{ planPreview.daysLeft }} 天
            </p>
            <p v-if="planPreview.summary" class="plan-preview__summary">{{ planPreview.summary }}</p>
            <ol class="plan-preview__phases">
              <li v-for="(phase, pi) in planPreview.phases" :key="`${pi}-${phase.title}`" class="plan-preview__phase">
                <div class="plan-preview__phase-title">
                  {{ phase.title }}
                  <span v-if="phase.dueTime" class="plan-preview__due">{{ phase.dueTime }}</span>
                </div>
                <p v-if="phase.description" class="plan-preview__phase-desc">{{ phase.description }}</p>
                <ul v-if="phase.steps?.length" class="plan-preview__steps">
                  <li v-for="(step, si) in phase.steps" :key="`${pi}-${si}-${step.title}`">
                    <span class="plan-preview__step-title">{{ step.title }}</span>
                    <span v-if="step.dueTime" class="plan-preview__due"> {{ step.dueTime }}</span>
                    <span v-if="step.description" class="plan-preview__step-desc"> — {{ step.description }}</span>
                  </li>
                </ul>
              </li>
            </ol>
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.task-page {
  --tp-ink: var(--color-text-primary);
  --tp-lift: 0 10px 32px rgba(6, 36, 64, 0.09), 0 2px 6px rgba(6, 36, 64, 0.04);
  --tp-glass: rgba(255, 255, 255, 0.72);
  --tp-glass-border: rgba(255, 255, 255, 0.7);
  --tp-ease: cubic-bezier(0.16, 1, 0.3, 1);
}

.task-page__grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.22;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.45'/%3E%3C/svg%3E");
  mix-blend-mode: soft-light;
}

/* —— Asymmetric pulse metrics (anti 4-card row) —— */
.tp-pulse {
  display: grid;
  grid-template-columns: minmax(140px, 0.85fr) minmax(0, 2.15fr);
  gap: 16px;
  align-items: stretch;
  margin-bottom: var(--space-5);
}

.tp-pulse__hero {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 20px 22px 22px;
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(8, 145, 178, 0.14) 0%, rgba(255, 255, 255, 0.88) 62%);
  border: 1px solid rgba(8, 145, 178, 0.2);
  box-shadow: var(--shadow-sm), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(12px);
}

.tp-pulse__value {
  font-size: clamp(2.25rem, 1.8rem + 1.4vw, 3rem);
  font-weight: 700;
  letter-spacing: -0.06em;
  font-variant-numeric: tabular-nums;
  line-height: 0.95;
  color: var(--color-accent-deep);
}

.tp-pulse__label {
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--color-text-secondary);
}

.tp-pulse__rail {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border-radius: 18px;
  background: var(--tp-glass);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.tp-pulse__item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  margin: 0;
  padding: 18px 16px 20px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  position: relative;
  transition: background 0.22s var(--tp-ease), transform 0.15s var(--tp-ease);
}

.tp-pulse__item + .tp-pulse__item {
  border-left: 1px solid rgba(6, 36, 64, 0.06);
}

.tp-pulse__item:hover {
  background: rgba(255, 255, 255, 0.55);
}

.tp-pulse__item:active {
  transform: scale(0.985);
}

.tp-pulse__item:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: -2px;
  z-index: 1;
}

.tp-pulse__item.is-on {
  background: rgba(255, 255, 255, 0.92);
}

.tp-pulse__item.is-on::after {
  content: '';
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 0;
  height: 2px;
  border-radius: 2px 2px 0 0;
  background: var(--color-accent);
}

.tp-pulse__item--warn {
  cursor: default;
}

.tp-pulse__item--warn.is-on {
  background: rgba(224, 85, 69, 0.05);
}

.tp-pulse__item--warn.is-on::after {
  background: var(--color-red, #e05545);
}

.tp-pulse__item--warn .tp-pulse__item-val {
  color: var(--color-text-primary);
}

.tp-pulse__item--warn.is-on .tp-pulse__item-val {
  color: var(--color-red, #e05545);
}

.tp-pulse__item-val {
  font-size: 1.45rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  color: var(--color-text-primary);
}

.tp-pulse__item-lab {
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--color-text-dim);
}

.task-view__toolbar {
  margin-bottom: var(--space-4);
}

.task-view__prio {
  width: 120px;
}

.affair-tabs {
  display: inline-flex;
  padding: 4px;
  border-radius: 12px;
  background: var(--tp-glass);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(10px);
}

.affair-tab {
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 9px;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.22s var(--tp-ease), color 0.22s ease, transform 0.15s var(--tp-ease), box-shadow 0.22s ease;
}

.affair-tab:hover {
  color: var(--color-text-primary);
}

.affair-tab:active {
  transform: scale(0.97);
}

.affair-tab:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.affair-tab.is-active {
  color: #fff;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-deep) 100%);
  box-shadow: 0 4px 14px rgba(8, 145, 178, 0.28);
}

.space-back {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 10px;
  padding: 4px 8px 4px 4px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-accent-deep);
  cursor: pointer;
  transition: color 0.15s ease, transform 0.15s var(--tp-ease), background 0.15s ease;
}

.space-back:hover {
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.space-back:active {
  transform: translateX(-2px);
}

.wb-header__title--space {
  font-size: clamp(1.85rem, 1.4rem + 1.6vw, 2.5rem);
  letter-spacing: -0.045em;
  line-height: 1.12;
  text-wrap: balance;
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
  border-radius: 14px;
  background: var(--tp-glass);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(12px);
}

.status-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.22s var(--tp-ease), color 0.22s ease, transform 0.15s var(--tp-ease), box-shadow 0.22s ease;
}

.status-tab__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-text-dim);
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.2s var(--tp-ease);
}

.status-tab--s0 .status-tab__dot { background: #b8922a; }
.status-tab--s1 .status-tab__dot { background: var(--color-accent); }
.status-tab--s2 .status-tab__dot { background: var(--color-green, #22a06b); }
.status-tab--s3 .status-tab__dot { background: var(--color-text-muted, #94a3b8); }

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
  color: var(--color-text-primary);
  background: #fff;
  box-shadow: var(--shadow-xs);
}

.status-tab.is-active .status-tab__dot {
  transform: scale(1.2);
  animation: tp-dot-breathe 2.4s ease-in-out infinite;
}

.status-tab--s0.is-active { color: #7a6210; }
.status-tab--s1.is-active { color: var(--color-accent-deep); }
.status-tab--s2.is-active { color: var(--color-green, #16794c); }
.status-tab--s3.is-active { color: var(--color-text-secondary); }

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
  transition: background 0.2s ease, color 0.2s ease;
}

.status-tab.is-active .status-tab__count {
  color: inherit;
  background: var(--color-accent-soft);
}

@keyframes tp-dot-breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

.task-panel {
  border-radius: 18px;
  background: var(--tp-glass);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(14px);
  overflow: hidden;
}

.task-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(6, 36, 64, 0.06);
}

.task-panel__heading {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.task-panel__title {
  margin: 0;
  font-size: 1.02rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--color-text-primary);
}

.task-panel__count {
  font-size: 0.78rem;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-dim);
}

.task-panel__add {
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-accent-deep);
  font-size: 0.8125rem;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.15s var(--tp-ease), color 0.15s ease;
}

.task-panel__add:hover {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.task-panel__add:active {
  transform: scale(0.97);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 6px 10px 12px;
  min-height: 200px;
}

.task-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 56px 16px;
  text-align: center;
}

.task-list__empty-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 650;
  color: var(--color-text-secondary);
}

.task-list__empty-desc {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-text-dim);
}

.task-card {
  --accent-bar: var(--color-accent);
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 4px 0;
  padding: 14px 14px 14px 16px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid transparent;
  overflow: hidden;
  animation: task-card-in 0.4s var(--tp-ease) both;

  transition:
    background 0.22s var(--tp-ease),
    border-color 0.22s ease,
    box-shadow 0.25s var(--tp-ease),
    transform 0.25s var(--tp-ease);
}

.task-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  background: var(--accent-bar);
  border-radius: 0 2px 2px 0;
  opacity: 0.75;
  transition: opacity 0.2s ease, top 0.2s ease, bottom 0.2s ease;
}

.task-card--s0 { --accent-bar: #b8922a; }
.task-card--s1 { --accent-bar: var(--color-accent); }
.task-card--s2 { --accent-bar: var(--color-green, #22a06b); }
.task-card--s3 { --accent-bar: var(--color-text-muted, #94a3b8); }

.task-card:hover {
  background: #fff;
  border-color: rgba(6, 36, 64, 0.06);
  box-shadow: var(--tp-lift);
  transform: translateY(-1px);
}

.task-card:hover::before {
  opacity: 1;
  top: 10px;
  bottom: 10px;
}

.task-card--overdue {
  background: linear-gradient(90deg, rgba(224, 85, 69, 0.05) 0%, transparent 40%);
}

.task-card--overdue::before {
  background: var(--color-red, #e05545);
}

@keyframes task-card-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.task-card__main {
  display: flex;
  flex-direction: column;
  gap: 7px;
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
  opacity: 0;
  transform: translateX(4px);
  transition: opacity 0.2s var(--tp-ease), transform 0.2s var(--tp-ease);
}

.task-card:hover .action-btns,
.task-card:focus-within .action-btns {
  opacity: 1;
  transform: translateX(0);
}

.task-card .btn-action {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  border: 1px solid transparent;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s var(--tp-ease);
}

.task-card .btn-action:active:not(:disabled) {
  transform: scale(0.94);
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
  border-color: rgba(8, 145, 178, 0.25);
}

.task-card .btn-action--delete {
  color: var(--color-text-dim);
}

.task-card .btn-action--delete:hover {
  color: var(--color-red);
  background: var(--color-red-soft);
  border-color: rgba(224, 85, 69, 0.3);
}

.task-card__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 650;
  letter-spacing: -0.025em;
  line-height: 1.4;
  color: var(--color-text-primary);
  text-wrap: pretty;
}

.task-card__desc {
  margin: 0;
  max-width: 65ch;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.task-tag {
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--color-text-body);
  background: var(--color-surface-muted);
}

/* —— Meta chips (attachments / due) —— */
.task-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 0;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid rgba(6, 36, 64, 0.08);
  background: rgba(6, 36, 64, 0.03);
  font-size: 0.72rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-secondary);
  line-height: 1;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease, transform 0.15s var(--tp-ease);
}

.meta-chip svg {
  flex-shrink: 0;
  opacity: 0.75;
}

.meta-chip--project {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
  border-color: rgba(8, 145, 178, 0.16);
}

.meta-chip--files {
  margin: 0;
  font: inherit;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-accent-deep);
  background: rgba(8, 145, 178, 0.08);
  border-color: rgba(8, 145, 178, 0.16);
}

.meta-chip--files:hover {
  background: rgba(8, 145, 178, 0.14);
  border-color: rgba(8, 145, 178, 0.28);
  transform: translateY(-1px);
}

.meta-chip--files:active {
  transform: scale(0.98);
}

.meta-chip--due {
  color: var(--color-text-body);
  background: #fff;
  border-color: rgba(6, 36, 64, 0.1);
}

.meta-chip--due.is-overdue {
  color: var(--color-red, #e05545);
  background: rgba(224, 85, 69, 0.08);
  border-color: rgba(224, 85, 69, 0.28);
}

.files-modal__hint {
  margin-bottom: 12px;
}

.files-modal__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(60vh, 420px);
  overflow: auto;
}

.files-modal__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  transition: background 0.15s ease, box-shadow 0.15s ease;
}

.files-modal__item:hover {
  background: #fff;
  box-shadow: var(--shadow-xs);
}

.files-modal__info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.files-modal__name {
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.files-modal__name:hover {
  color: var(--color-accent);
}

.files-modal__meta {
  font-size: 0.72rem;
  color: var(--color-text-dim);
}

.files-modal__actions {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.files-modal__act {
  margin: 0;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-accent-deep);
  cursor: pointer;
}

.files-modal__act:hover {
  color: var(--color-accent);
}

.files-modal__act--danger {
  color: var(--color-red);
}

.files-modal__act--danger:hover {
  color: var(--color-red);
  opacity: 0.85;
}

.attach-review {
  margin: 0 0 var(--space-4);
  padding: var(--space-4) var(--space-5);
  border: 1px solid var(--color-border-light);
  border-radius: 16px;
  background: var(--tp-glass);
  box-shadow: var(--shadow-xs), inset 0 1px 0 var(--tp-glass-border);
  backdrop-filter: blur(12px);
}

.attach-review__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 14px;
}

.attach-review__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.attach-review__meta {
  font-size: 0.75rem;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-dim);
}

.attach-review__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attach-review__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--color-surface-muted);
  border: 1px solid transparent;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.attach-review__item:hover {
  background: #fff;
  border-color: var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.attach-review__info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.attach-review__actions {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.attach-review__act {
  margin: 0;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-accent-deep);
  cursor: pointer;
}

.attach-review__act:hover {
  color: var(--color-accent);
}

.attach-review__name,
.task-attach__name {
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-accent-deep);
  cursor: pointer;
}

.attach-review__name:hover,
.task-attach__name:hover {
  text-decoration: underline;
  text-underline-offset: 2px;
}

.attach-review__sub {
  font-size: 0.72rem;
  color: var(--color-text-dim);
}

.attach-review__empty,
.task-attach__hint {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-dim);
}

.task-attach {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-attach__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-attach__item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.task-attach__size {
  font-size: 0.72rem;
  color: var(--color-text-dim);
}

.task-attach__dl,
.task-attach__del {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-accent-deep);
}

.task-attach__del {
  color: var(--color-red);
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

@media (max-width: 900px) {
  .tp-pulse {
    grid-template-columns: 1fr;
  }
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

  .tp-pulse__rail {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .tp-pulse__item {
    padding: 14px 12px 16px;
  }

  .task-card:hover {
    transform: none;
  }

  .task-card .action-btns {
    opacity: 1;
    transform: none;
  }
}

.plan-dialog__hint {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-dialog__deadline-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-dialog__actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.plan-preview {
  border-top: 1px solid rgba(6, 36, 64, 0.08);
  padding-top: 14px;
  max-height: 360px;
  overflow: auto;
}

.plan-preview__meta {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-preview__summary {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--tp-ink);
}

.plan-preview__phases {
  margin: 0;
  padding-left: 1.2em;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-preview__phase-title {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}

.plan-preview__due {
  font-weight: 500;
  font-size: 12px;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-preview__phase-desc {
  margin: 4px 0 6px;
  font-size: 12px;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-preview__steps {
  margin: 0;
  padding-left: 1.1em;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary, #5a6a7a);
}

.plan-preview__step-title {
  color: var(--tp-ink);
}

@media (prefers-reduced-motion: reduce) {
  .affair-tab,
  .status-tab,
  .space-back,
  .task-card,
  .task-panel__add,
  .tp-pulse__item,
  .meta-chip--files {
    transition: none;
    animation: none;
  }

  .status-tab.is-active .status-tab__dot {
    animation: none;
  }

  .task-card:hover {
    transform: none;
  }
}
</style>

