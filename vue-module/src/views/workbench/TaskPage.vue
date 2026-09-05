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
  removeTaskAttachmentApi,
  setProjectArchivedApi,
  updateAttachmentStatusApi,
  updateTaskApi,
  uploadTaskAttachmentApi,
} from '@/apis/workbench/TaskApi'
import { planApplyApi, planPreviewApi } from '@/apis/ai/AiApi'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import EditorView from '@/components/EditorView.vue'
import {
  calcCompletion,
  countTaskStatus,
  completionGaugeOption,
  statusDoughnutOption,
} from '@/utils/wbCharts'
import { openKkFileViewPreview } from '@/utils/kkFileView'
import {
  LeftOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  PaperClipOutlined,
  ClockCircleOutlined,
  InboxOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const NODE_PROJECT = 'project'
const NODE_PHASE = 'phase'
const NODE_TASK = 'task'

/** 兼容缺省 / 下划线字段，缺省按叶子任务处理 */
function nodeTypeOf(t) {
  const raw = t?.nodeType ?? t?.node_type ?? ''
  const v = String(raw).trim().toLowerCase()
  if (v === NODE_PROJECT || v === NODE_PHASE || v === NODE_TASK) return v
  return NODE_TASK
}
function isLeafTask(t) {
  return nodeTypeOf(t) === NODE_TASK
}
function isPhaseNode(t) {
  return nodeTypeOf(t) === NODE_PHASE
}
function isProjectNode(t) {
  return nodeTypeOf(t) === NODE_PROJECT
}

const STATUS_COLUMNS = [
  { key: '0', label: '待办' },
  { key: '1', label: '进行中' },
  { key: '2', label: '已完成' },
  { key: '3', label: '已取消' },
]

const filterProjectId = ref(
  route.query.projectId != null && route.query.projectId !== ''
    ? Number(route.query.projectId)
    : null,
)
const syncingQuery = ref(false)

const inProjectSpace = computed(() => filterProjectId.value != null)

/* ---------- 统一任务树 ---------- */
const taskLoading = ref(false)
const taskSubmitting = ref(false)
const taskDialogVisible = ref(false)
const taskIsEdit = ref(false)
const tasks = ref([])
const taskSearchQuery = ref('')
const filterPriority = ref(null)
/** null=全部 | independent=无父任务 | bound=有父任务 */
const filterBinding = ref(null)

const taskById = computed(() => {
  const map = new Map()
  for (const t of tasks.value) map.set(Number(t.taskId), t)
  return map
})

const childrenByParent = computed(() => {
  const map = new Map()
  for (const t of tasks.value) {
    if (t.parentTaskId == null || t.parentTaskId === '') continue
    const pid = Number(t.parentTaskId)
    if (!map.has(pid)) map.set(pid, [])
    map.get(pid).push(t)
  }
  return map
})

/** 顶层任务节点（nodeType=project，展示用 name = title） */
const projects = computed(() =>
  tasks.value
    .filter((t) => isProjectNode(t))
    .slice()
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    .map((t) => ({
      ...t,
      nodeType: NODE_PROJECT,
      name: t.title,
    })),
)

const projectMap = computed(() => {
  const map = new Map()
  for (const p of projects.value) map.set(Number(p.taskId), p.title || p.name || '')
  return map
})

const activeProjects = computed(() =>
  projects.value.filter((p) => String(p.archived) !== '1'),
)

function isProjectArchived(project) {
  return project != null && String(project.archived) === '1'
}

function getTaskById(id) {
  if (id == null || id === '') return null
  return taskById.value.get(Number(id)) || null
}

/** 沿 parentTaskId 向上找到顶层节点 */
function findAncestorProject(taskLike) {
  if (!taskLike) return null
  let cur
  if (typeof taskLike === 'object') {
    cur =
      taskLike.taskId != null
        ? getTaskById(taskLike.taskId) || taskLike
        : taskLike
  } else {
    cur = getTaskById(taskLike)
  }
  const seen = new Set()
  while (cur) {
    const id = Number(cur.taskId)
    if (!Number.isNaN(id)) {
      if (seen.has(id)) break
      seen.add(id)
    }
    if (isProjectNode(cur)) return cur
    if (cur.parentTaskId == null || cur.parentTaskId === '') break
    cur = getTaskById(cur.parentTaskId)
  }
  return null
}

function collectDescendants(rootId) {
  const root = Number(rootId)
  const out = []
  const queue = [...(childrenByParent.value.get(root) || [])]
  const seen = new Set()
  while (queue.length) {
    const node = queue.shift()
    const id = Number(node.taskId)
    if (seen.has(id)) continue
    seen.add(id)
    out.push(node)
    const kids = childrenByParent.value.get(id)
    if (kids?.length) queue.push(...kids)
  }
  return out
}

function leafTasksUnder(rootId) {
  return collectDescendants(rootId).filter((t) => isLeafTask(t))
}

function hasProjectAncestor(task) {
  return findAncestorProject(task) != null
}

function phaseTitleOf(task) {
  if (!task?.parentTaskId) return ''
  const parent = getTaskById(task.parentTaskId)
  if (parent && isPhaseNode(parent)) return parent.title || ''
  return ''
}

function projectNameOf(taskLike) {
  const project = findAncestorProject(taskLike)
  if (!project) return ''
  return project.title || project.name || projectMap.value.get(Number(project.taskId)) || ''
}

const currentProject = computed(() => {
  if (!inProjectSpace.value) return null
  const id = Number(filterProjectId.value)
  return projects.value.find((p) => Number(p.taskId) === id) || getTaskById(id)
})

const filterProjectName = computed(() => {
  if (!inProjectSpace.value) return ''
  return (
    currentProject.value?.title ||
    currentProject.value?.name ||
    projectMap.value.get(Number(filterProjectId.value)) ||
    `任务 #${filterProjectId.value}`
  )
})

const projectAttachmentsReadonly = computed(
  () => inProjectSpace.value && isProjectArchived(currentProject.value),
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
          ? '已归档 · 附件只读，可下载存档'
          : '任务组 · 阶段用父任务、步骤用子任务；看板只推进叶子任务'),
    }
  }
  return {
    title: '事务',
    desc: '任务与子任务统一管理；可建父任务拆分子任务，看板推进叶子任务。',
  }
})

function buildQuery() {
  const query = {}
  if (filterProjectId.value != null && !Number.isNaN(Number(filterProjectId.value))) {
    query.projectId = String(filterProjectId.value)
  }
  return query
}

function syncRouteQuery() {
  const next = buildQuery()
  const curProject =
    route.query.projectId != null && route.query.projectId !== ''
      ? String(route.query.projectId)
      : undefined
  const nextProject = next.projectId

  if (curProject === nextProject) return

  syncingQuery.value = true
  router.replace({ path: route.path, query: next }).finally(() => {
    syncingQuery.value = false
  })
}

function viewProjectTasks(projectTaskId) {
  filterProjectId.value = Number(projectTaskId)
}

function leaveProjectSpace() {
  filterProjectId.value = null
}

watch(filterProjectId, () => {
  syncRouteQuery()
})

watch(
  () => route.query.projectId,
  () => {
    if (syncingQuery.value) return
    filterProjectId.value =
      route.query.projectId != null && route.query.projectId !== ''
        ? Number(route.query.projectId)
        : null
  },
)

const taskForm = reactive({
  taskId: null,
  nodeType: NODE_TASK,
  parentTaskId: null,
  title: '',
  description: '',
  status: '0',
  priority: 0,
  dueTime: null,
  tags: '',
  displayOrder: 0,
  remark: '',
})

/** 看板只展示 leaf task；任务组内取根下子孙 leaf */
const projectScopedTasks = computed(() => {
  if (!inProjectSpace.value) return tasks.value.filter((t) => isLeafTask(t))
  return leafTasksUnder(filterProjectId.value)
})

/** 任务组：阶段 + 下属步骤的紧凑树 */
const projectPhaseTree = computed(() => {
  if (!inProjectSpace.value) return []
  const rootId = Number(filterProjectId.value)
  const phases = (childrenByParent.value.get(rootId) || [])
    .filter((t) => isPhaseNode(t))
    .slice()
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
  return phases.map((phase) => ({
    ...phase,
    steps: (childrenByParent.value.get(Number(phase.taskId)) || [])
      .filter((t) => isLeafTask(t))
      .slice()
      .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)),
  }))
})

/** 可选父节点：活跃顶层 +（任务组内）本阶段 */
const parentSelectOptions = computed(() => {
  const opts = []
  for (const p of activeProjects.value) {
    opts.push({
      value: Number(p.taskId),
      label: p.title || p.name,
      kind: 'project',
    })
  }
  if (inProjectSpace.value && filterProjectId.value != null) {
    const rootId = Number(filterProjectId.value)
    const phases = (childrenByParent.value.get(rootId) || []).filter((t) => isPhaseNode(t))
    for (const ph of phases) {
      if (opts.some((o) => o.value === Number(ph.taskId))) continue
      opts.push({
        value: Number(ph.taskId),
        label: `${filterProjectName.value} / ${ph.title}`,
        kind: 'phase',
      })
    }
  }
  return opts
})

/** 看板顶栏：快捷进入未归档父任务 */
const projectQuickEntries = computed(() =>
  activeProjects.value.slice(0, 8).map((p) => ({
    taskId: Number(p.taskId),
    title: p.title || p.name || `任务 #${p.taskId}`,
  })),
)

const filteredTasks = computed(() => {
  const q = taskSearchQuery.value.trim().toLowerCase()
  return projectScopedTasks.value.filter((t) => {
    if (filterPriority.value != null && Number(t.priority) !== Number(filterPriority.value)) {
      return false
    }
    if (!inProjectSpace.value) {
      if (filterBinding.value === 'independent') {
        if (t.parentTaskId != null && t.parentTaskId !== '') return false
      } else if (filterBinding.value === 'bound') {
        if (!hasProjectAncestor(t)) return false
      }
    }
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

const projectCompletion = computed(() => calcCompletion(projectScopedTasks.value))

const projectGaugeOption = computed(() =>
  completionGaugeOption(projectCompletion.value.rate, { subtitle: '完成率' }),
)

const projectStatusChartOption = computed(() =>
  statusDoughnutOption(countTaskStatus(projectScopedTasks.value), { title: 'STATUS' }),
)

/** 各父任务完成率：leaf task descendants（条目标题等展示） */
const projectProgressMap = computed(() => {
  const result = new Map()
  for (const p of projects.value) {
    const list = leafTasksUnder(p.taskId)
    const stats = calcCompletion(list)
    result.set(Number(p.taskId), stats)
  }
  return result
})

const emptyProjectProgress = {
  done: 0,
  cancelled: 0,
  eligible: 0,
  total: 0,
  rate: 0,
}

function projectProgressOf(projectTaskId) {
  return projectProgressMap.value.get(Number(projectTaskId)) || emptyProjectProgress
}

function resetTaskForm(defaults = {}) {
  Object.assign(taskForm, {
    taskId: null,
    nodeType: NODE_TASK,
    parentTaskId: null,
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

const ATTACH_STATUS = [
  { key: '0', label: '未处理' },
  { key: '1', label: '已处理' },
  { key: '2', label: '处理完成' },
]

function resolveAttachmentStatus(file) {
  if (!file) return '0'
  const s = file.status != null ? String(file.status).trim() : ''
  if (s === '0' || s === '1' || s === '2') return s
  const v = file.handled
  if (v === true || v === 1 || v === '1' || v === 'true') return '1'
  return '0'
}

function parseAttachments(raw) {
  let list = []
  if (!raw) list = []
  else if (Array.isArray(raw)) list = raw
  else if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      list = Array.isArray(parsed) ? parsed : []
    } catch {
      list = []
    }
  }
  return list.map((f) => ({ ...f, status: resolveAttachmentStatus(f) }))
}

function attachmentCount(task) {
  return parseAttachments(task?.attachments).length
}

function attachmentStatusLabel(file) {
  const key = resolveAttachmentStatus(file)
  return ATTACH_STATUS.find((x) => x.key === key)?.label || '未处理'
}

function attachmentStatusClass(file) {
  const key = resolveAttachmentStatus(file)
  if (key === '2') return 'attach-badge--done'
  if (key === '1') return 'attach-badge--doing'
  return 'attach-badge--todo'
}

function formatFileSize(size) {
  const n = Number(size)
  if (!n || n < 0) return ''
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

/** 附件只读：祖先顶层 archived === '1' */
function isTaskAttachmentsReadonly(taskLike) {
  if (taskLike == null || taskLike === '') return false
  if (typeof taskLike === 'object' && isProjectNode(taskLike)) {
    return isProjectArchived(taskLike)
  }
  const project = findAncestorProject(taskLike)
  if (project) return isProjectArchived(project)
  // 表单仅有 parentTaskId 时
  if (typeof taskLike === 'object' && taskLike.parentTaskId != null) {
    return isProjectArchived(findAncestorProject({ parentTaskId: taskLike.parentTaskId }))
  }
  return false
}

/* ---------- 任务附件 ---------- */
const taskAttachments = ref([])

const taskFormAttachmentsReadonly = computed(() =>
  isTaskAttachmentsReadonly({
    taskId: taskForm.taskId,
    nodeType: taskForm.nodeType,
    parentTaskId: taskForm.parentTaskId,
  }),
)

const projectAttachmentList = ref([])
const projectAttachmentLoading = ref(false)

const projectAttachmentStatusCounts = computed(() => {
  const counts = { '0': 0, '1': 0, '2': 0 }
  for (const f of projectAttachmentList.value) {
    const key = resolveAttachmentStatus(f)
    counts[key] = (counts[key] || 0) + 1
  }
  return counts
})

async function loadProjectAttachments() {
  if (!inProjectSpace.value) {
    projectAttachmentList.value = []
    return
  }
  projectAttachmentLoading.value = true
  try {
    const result = await listProjectAttachmentsApi(filterProjectId.value)
    const list = result?.data || []
    projectAttachmentList.value = list.map((f) => ({ ...f, status: resolveAttachmentStatus(f) }))
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

const filesModalReadonly = computed(() => isTaskAttachmentsReadonly(filesModalTask.value))

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
  return removeAttachment(attachmentId, { taskId: task.taskId, task })
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

function resolveReadonlyTask(ctx = {}) {
  if (ctx.task) return ctx.task
  if (ctx.taskId != null) return getTaskById(ctx.taskId) || { taskId: ctx.taskId, parentTaskId: ctx.parentTaskId }
  return {
    taskId: taskForm.taskId,
    nodeType: taskForm.nodeType,
    parentTaskId: taskForm.parentTaskId,
  }
}

async function removeAttachment(attachmentId, ctx = {}) {
  const taskId = ctx.taskId ?? taskForm.taskId
  if (!taskId || isTaskAttachmentsReadonly(resolveReadonlyTask(ctx))) return
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

async function setAttachmentStatus(file, status, ctx = {}) {
  const taskId = ctx.taskId ?? file?.taskId ?? taskForm.taskId
  const attachmentId = file?.id
  const next = String(status)
  if (!taskId || !attachmentId || isTaskAttachmentsReadonly(resolveReadonlyTask({ ...ctx, taskId }))) {
    return
  }
  if (resolveAttachmentStatus(file) === next) return
  try {
    await updateAttachmentStatusApi(taskId, attachmentId, next)
    const label = ATTACH_STATUS.find((x) => x.key === next)?.label || next
    message.success(`已标记为${label}`)
    await loadTasks()
    syncAttachmentViews(taskId)
    if (inProjectSpace.value) await loadProjectAttachments()
  } catch (error) {
    message.error(error.message || '更新失败')
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
  await loadTasks()
}

const planDialogVisible = ref(false)
const planPreviewing = ref(false)
const planApplying = ref(false)
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
    message.warning('请先选择截止时间')
    return
  }
  if (dayjs(planForm.deadline).isBefore(dayjs(), 'day')) {
    message.warning('截止时间不能早于今天')
    return
  }
  planPreviewing.value = true
  try {
    const result = await planPreviewApi({
      projectTaskId: Number(filterProjectId.value),
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
      projectTaskId: Number(filterProjectId.value),
      planBatchId: planPreview.value.planBatchId,
      phases: planPreview.value.phases,
    })
    const created = result?.data?.created ?? 0
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

function openCreateTask(status = '0') {
  taskIsEdit.value = false
  taskAttachments.value = []
  resetTaskForm({
    status: String(status),
    nodeType: NODE_TASK,
    parentTaskId: filterProjectId.value != null ? Number(filterProjectId.value) : null,
  })
  taskDialogVisible.value = true
}

function openCreatePhase() {
  if (!inProjectSpace.value || filterProjectId.value == null) return
  taskIsEdit.value = false
  taskAttachments.value = []
  resetTaskForm({
    nodeType: NODE_PHASE,
    parentTaskId: Number(filterProjectId.value),
    status: '0',
    priority: 0,
  })
  taskDialogVisible.value = true
}

function openEditTask(row) {
  taskIsEdit.value = true
  taskAttachments.value = parseAttachments(row.attachments)
  resetTaskForm({
    taskId: row.taskId,
    nodeType: row.nodeType || NODE_TASK,
    parentTaskId: row.parentTaskId != null ? Number(row.parentTaskId) : null,
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
  const nodeType = taskForm.nodeType || NODE_TASK
  return {
    taskId: taskForm.taskId,
    nodeType,
    parentTaskId: taskForm.parentTaskId != null ? Number(taskForm.parentTaskId) : null,
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

const taskDialogTitle = computed(() => {
  if (taskIsEdit.value) {
    if (taskForm.nodeType === NODE_PHASE) return '编辑阶段'
    if (taskForm.nodeType === NODE_PROJECT) return '编辑父任务'
    return '编辑任务'
  }
  if (taskForm.nodeType === NODE_PHASE) return '新建阶段'
  return '新建任务'
})

async function submitTaskForm() {
  if (!taskForm.title?.trim()) {
    message.warning(taskForm.nodeType === NODE_PHASE ? '请填写阶段标题' : '请填写任务标题')
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

/* ---------- 父任务（nodeType=project）创建 / 编辑 / 归档 ---------- */
const projectSubmitting = ref(false)
const projectDialogVisible = ref(false)
const projectIsEdit = ref(false)

const projectForm = reactive({
  taskId: null,
  title: '',
  description: '',
  archived: '0',
  displayOrder: 0,
  remark: '',
})

function resetProjectForm() {
  Object.assign(projectForm, {
    taskId: null,
    title: '',
    description: '',
    archived: '0',
    displayOrder: 0,
    remark: '',
  })
}

function openCreateProject() {
  projectIsEdit.value = false
  resetProjectForm()
  projectDialogVisible.value = true
}

function openEditProject(row) {
  if (!row) return
  projectIsEdit.value = true
  Object.assign(projectForm, {
    taskId: row.taskId,
    title: row.title || row.name || '',
    description: row.description || '',
    archived: row.archived != null ? String(row.archived) : '0',
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  projectDialogVisible.value = true
}

async function submitProjectForm() {
  if (!projectForm.title?.trim()) {
    message.warning('请填写标题')
    return
  }
  projectSubmitting.value = true
  try {
    const payload = {
      taskId: projectForm.taskId,
      nodeType: NODE_PROJECT,
      parentTaskId: null,
      title: projectForm.title.trim(),
      description: projectForm.description?.trim() || '',
      archived: String(projectForm.archived ?? '0'),
      displayOrder: Number(projectForm.displayOrder) || 0,
      remark: projectForm.remark?.trim() || '',
      status: '0',
    }
    if (projectIsEdit.value) await updateTaskApi(payload)
    else await addTaskApi(payload)
    message.success('操作成功')
    projectDialogVisible.value = false
    await loadTasks()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    projectSubmitting.value = false
  }
}

async function removeProject(id) {
  try {
    await deleteTasksApi(id)
    message.success('删除成功')
    if (filterProjectId.value != null && Number(filterProjectId.value) === Number(id)) {
      leaveProjectSpace()
    }
    await loadTasks()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function toggleArchive(row) {
  if (!row?.taskId) return
  const next = String(row.archived) === '1' ? '0' : '1'
  try {
    await setProjectArchivedApi(row.taskId, next)
    message.success(next === '1' ? '已归档' : '已恢复进行中')
    await loadTasks()
  } catch (error) {
    message.error(error.message || '状态更新失败')
  }
}

async function refreshCurrent() {
  await loadTasks()
}

onMounted(async () => {
  await loadTasks()
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
            返回
          </button>
          <h1 class="wb-header__title" :class="{ 'wb-header__title--space': inProjectSpace }">
            {{ headerCopy.title }}
          </h1>
          <p class="wb-header__desc">{{ headerCopy.desc }}</p>
        </div>
        <div class="wb-header__actions">
          <template v-if="inProjectSpace && currentProject">
            <button
              v-permission="'task:modify'"
              type="button"
              class="wb-btn wb-btn--ghost"
              @click="openEditProject(currentProject)"
            >
              <EditOutlined />
              编辑
            </button>
            <button
              v-permission="'task:modify'"
              type="button"
              class="wb-btn wb-btn--ghost"
              @click="toggleArchive(currentProject)"
            >
              <InboxOutlined />
              {{ String(currentProject.archived) === '1' ? '恢复' : '归档' }}
            </button>
            <a-popconfirm
              v-permission="'task:remove'"
              title="确认删除该父任务及其子任务？"
              @confirm="removeProject(currentProject.taskId)"
            >
              <button type="button" class="wb-btn wb-btn--ghost">
                <DeleteOutlined />
                删除
              </button>
            </a-popconfirm>
          </template>
          <button type="button" class="wb-btn wb-btn--ghost" @click="refreshCurrent">刷新</button>
        </div>
      </header>

      <!-- 任务看板 -->
      <div class="task-view">
        <section
          v-if="inProjectSpace"
          class="tp-charts"
          aria-label="任务组进度"
        >
          <article class="tp-chart-card tp-chart-card--gauge">
            <v-chart class="tp-chart" :option="projectGaugeOption" autoresize />
            <p class="tp-chart-meta">
              <span>{{ projectCompletion.done }} / {{ projectCompletion.eligible }} 已完成</span>
              <span v-if="projectCompletion.cancelled">· 已取消 {{ projectCompletion.cancelled }}</span>
            </p>
          </article>
          <article class="tp-chart-card">
            <v-chart class="tp-chart" :option="projectStatusChartOption" autoresize />
            <p class="tp-chart-meta">
              <span>本组 {{ taskStats.total }} 项任务</span>
            </p>
          </article>
        </section>

        <div class="tp-pulse" aria-label="任务概览">
          <div class="tp-pulse__hero">
            <span class="tp-pulse__value">{{ taskStats.total }}</span>
            <span class="tp-pulse__label">{{ inProjectSpace ? '本组任务' : '全部任务' }}</span>
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

        <section
          v-if="!inProjectSpace && projectQuickEntries.length"
          class="project-strip"
          aria-label="快捷进入父任务"
        >
          <span class="project-strip__label">父任务</span>
          <div class="project-strip__chips">
            <button
              v-for="p in projectQuickEntries"
              :key="p.taskId"
              type="button"
              class="project-strip__chip"
              :title="`${projectProgressOf(p.taskId).rate}% · ${projectProgressOf(p.taskId).done}/${projectProgressOf(p.taskId).eligible}`"
              @click="viewProjectTasks(p.taskId)"
            >
              <FolderOutlined />
              {{ p.title }}
            </button>
          </div>
        </section>

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
          <a-select
            v-if="!inProjectSpace"
            v-model:value="filterBinding"
            allow-clear
            placeholder="任务归属"
            class="task-view__binding"
          >
            <a-select-option value="independent">独立（无父节点）</a-select-option>
            <a-select-option value="bound">有父任务</a-select-option>
          </a-select>
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
            v-if="inProjectSpace"
            v-permission="'task:add'"
            type="button"
            class="wb-btn wb-btn--ghost"
            :disabled="projectAttachmentsReadonly"
            @click="openCreatePhase"
          >
            <PlusOutlined />
            新建阶段
          </button>
          <button
            v-if="!inProjectSpace"
            v-permission="'task:add'"
            type="button"
            class="wb-btn wb-btn--ghost"
            @click="openCreateProject"
          >
            <PlusOutlined />
            新建父任务
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

        <section
          v-if="inProjectSpace && projectPhaseTree.length"
          class="phase-tree"
          aria-label="阶段结构"
        >
          <h2 class="phase-tree__title">阶段结构</h2>
          <ul class="phase-tree__list">
            <li v-for="phase in projectPhaseTree" :key="phase.taskId" class="phase-tree__phase">
              <div class="phase-tree__phase-head">
                <span class="phase-tree__phase-name">{{ phase.title }}</span>
                <span class="phase-tree__phase-count">{{ phase.steps.length }} 步</span>
                <button
                  v-permission="'task:modify'"
                  type="button"
                  class="phase-tree__edit"
                  title="编辑阶段"
                  @click="openEditTask(phase)"
                >
                  <EditOutlined />
                </button>
              </div>
              <ul v-if="phase.steps.length" class="phase-tree__steps">
                <li v-for="step in phase.steps" :key="step.taskId" class="phase-tree__step">
                  <span class="phase-tree__step-status" :data-s="String(step.status ?? '0')" />
                  <span class="phase-tree__step-title">{{ step.title }}</span>
                </li>
              </ul>
              <p v-else class="phase-tree__empty">暂无步骤</p>
            </li>
          </ul>
        </section>

        <section v-if="inProjectSpace" class="attach-review" aria-label="附件审查">
          <div class="attach-review__head">
            <h2 class="attach-review__title">附件审查</h2>
            <span class="attach-review__meta">
              {{ projectAttachmentList.length }} 个文件
              <template v-if="projectAttachmentList.length">
                · {{ projectAttachmentStatusCounts['0'] }} 未处理
                · {{ projectAttachmentStatusCounts['1'] }} 已处理
                · {{ projectAttachmentStatusCounts['2'] }} 处理完成
              </template>
              <template v-if="projectAttachmentsReadonly"> · 已归档只读</template>
            </span>
          </div>
          <a-spin :spinning="projectAttachmentLoading">
            <ul v-if="projectAttachmentList.length" class="attach-review__list">
              <li
                v-for="file in projectAttachmentList"
                :key="`${file.taskId}-${file.id}`"
                class="attach-review__item"
                :class="`is-status-${resolveAttachmentStatus(file)}`"
              >
                <div class="attach-review__info">
                  <div class="attach-review__title-row">
                    <button type="button" class="attach-review__name" :title="file.name" @click="previewAttachment(file)">
                      {{ file.name }}
                    </button>
                    <span class="attach-badge" :class="attachmentStatusClass(file)">
                      {{ attachmentStatusLabel(file) }}
                    </span>
                  </div>
                  <span class="attach-review__sub">
                    {{ file.taskTitle || `任务 #${file.taskId}` }}
                    <template v-if="formatFileSize(file.size)"> · {{ formatFileSize(file.size) }}</template>
                    <template v-if="file.createTime"> · {{ file.createTime }}</template>
                  </span>
                </div>
                <div class="attach-review__actions">
                  <button type="button" class="attach-review__act" @click="previewAttachment(file)">预览</button>
                  <button type="button" class="attach-review__act" @click="downloadAttachment(file)">下载</button>
                  <select
                    v-if="!projectAttachmentsReadonly"
                    v-permission="'task:modify'"
                    class="attach-status-select"
                    :value="resolveAttachmentStatus(file)"
                    :aria-label="`附件状态 · ${file.name || ''}`"
                    @change="setAttachmentStatus(file, $event.target.value, { taskId: file.taskId, task: currentProject })"
                  >
                    <option v-for="opt in ATTACH_STATUS" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
                  </select>
                </div>
              </li>
            </ul>
            <p v-else class="attach-review__empty">本组任务下还没有附件</p>
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
            <h2 class="wb-empty__title">
              {{ inProjectSpace ? '这组任务还是空的' : '还没有叶子任务' }}
            </h2>
            <p class="wb-empty__desc">
              <template v-if="inProjectSpace">
                为本组创建阶段或子任务；阶段下的步骤会出现在下方看板。
              </template>
              <template v-else-if="projects.length">
                看板只显示叶子任务。你已有 {{ projects.length }} 个父任务，可点上方芯片进入任务组。
              </template>
              <template v-else>
                先新建一条任务，或新建父任务后再拆阶段 / 子任务。
              </template>
            </p>
            <div class="wb-empty__actions">
              <button
                v-permission="'task:add'"
                type="button"
                class="wb-btn wb-btn--primary"
                @click="openCreateTask('0')"
              >
                新建任务
              </button>
              <button
                v-if="!inProjectSpace"
                v-permission="'task:add'"
                type="button"
                class="wb-btn wb-btn--ghost"
                @click="openCreateProject"
              >
                新建父任务
              </button>
            </div>
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
                  v-if="(!inProjectSpace && projectNameOf(task)) || (inProjectSpace && phaseTitleOf(task)) || attachmentCount(task) || task.dueTime"
                  class="task-card__meta"
                >
                  <span v-if="!inProjectSpace && projectNameOf(task)" class="meta-chip meta-chip--project">
                    <FolderOutlined />
                    {{ projectNameOf(task) }}
                  </span>
                  <span v-if="inProjectSpace && phaseTitleOf(task)" class="meta-chip meta-chip--phase">
                    {{ phaseTitleOf(task) }}
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
    </div>

    <DataOperationView
      v-model="taskDialogVisible"
      :title="taskDialogTitle"
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
              :placeholder="taskForm.nodeType === 'phase' ? '阶段名称' : '要完成什么？'"
              :maxlength="120"
              size="large"
            />
          </a-form-item>

          <a-form-item v-if="taskForm.nodeType !== 'phase'" label="状态" class="dialog-item">
            <a-select v-model:value="taskForm.status" style="width: 100%">
              <a-select-option v-for="col in STATUS_COLUMNS" :key="col.key" :value="col.key">
                {{ col.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-if="taskForm.nodeType !== 'phase'" label="优先级" class="dialog-item">
            <a-input-number v-model:value="taskForm.priority" :precision="0" style="width: 100%" />
          </a-form-item>

          <a-form-item
            v-if="inProjectSpace || taskIsEdit"
            label="节点类型"
            class="dialog-item"
          >
            <a-select
              v-model:value="taskForm.nodeType"
              :disabled="taskIsEdit || (!inProjectSpace && taskForm.nodeType === 'task')"
              style="width: 100%"
            >
              <a-select-option value="task">任务</a-select-option>
              <a-select-option v-if="inProjectSpace || taskForm.nodeType === 'phase'" value="phase">
                阶段
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="taskForm.nodeType !== 'phase' || !inProjectSpace"
            label="父节点"
            class="dialog-item"
          >
            <a-select
              v-model:value="taskForm.parentTaskId"
              allow-clear
              :disabled="taskForm.nodeType === 'phase'"
              placeholder="独立任务可不选；可挂到父任务或阶段下"
              style="width: 100%"
            >
              <a-select-option
                v-for="opt in parentSelectOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-if="taskForm.nodeType !== 'phase'" label="截止时间" class="dialog-item">
            <a-date-picker
              v-model:value="taskForm.dueTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
              placeholder="选填"
            />
          </a-form-item>

          <a-form-item v-if="taskForm.nodeType !== 'phase'" label="标签" class="dialog-item">
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

          <a-form-item
            v-if="taskForm.nodeType !== 'phase'"
            label="附件"
            class="dialog-item dialog-item--full"
          >
            <div class="task-attach">
              <p v-if="!taskIsEdit" class="task-attach__hint">先创建任务，再上传附件</p>
              <template v-else>
                <p v-if="taskFormAttachmentsReadonly" class="task-attach__hint">
                  上级任务组已归档，附件只读，可下载
                </p>
                <ul v-if="taskAttachments.length" class="task-attach__list">
                  <li v-for="file in taskAttachments" :key="file.id" class="task-attach__item">
                    <button type="button" class="task-attach__name" :title="file.name" @click="previewAttachment(file)">
                      {{ file.name }}
                    </button>
                    <span class="attach-badge" :class="attachmentStatusClass(file)">
                      {{ attachmentStatusLabel(file) }}
                    </span>
                    <span class="task-attach__size">{{ formatFileSize(file.size) }}</span>
                    <button type="button" class="task-attach__dl" @click="previewAttachment(file)">预览</button>
                    <button type="button" class="task-attach__dl" @click="downloadAttachment(file)">下载</button>
                    <select
                      v-if="!taskFormAttachmentsReadonly"
                      v-permission="'task:modify'"
                      class="attach-status-select"
                      :value="resolveAttachmentStatus(file)"
                      :aria-label="`附件状态 · ${file.name || ''}`"
                      @change="setAttachmentStatus(file, $event.target.value)"
                    >
                      <option v-for="opt in ATTACH_STATUS" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
                    </select>
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
        上级任务组已归档，附件只读，可预览与下载
      </p>
      <ul v-if="filesModalFiles.length" class="files-modal__list">
        <li
          v-for="file in filesModalFiles"
          :key="file.id || file.url"
          class="files-modal__item"
          :class="`is-status-${resolveAttachmentStatus(file)}`"
        >
          <div class="files-modal__info">
            <div class="attach-review__title-row">
              <button
                type="button"
                class="files-modal__name"
                :title="file.name"
                @click="previewAttachment(file)"
              >
                {{ file.name || '未命名文件' }}
              </button>
              <span class="attach-badge" :class="attachmentStatusClass(file)">
                {{ attachmentStatusLabel(file) }}
              </span>
            </div>
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
            <select
              v-if="!filesModalReadonly"
              v-permission="'task:modify'"
              class="attach-status-select"
              :value="resolveAttachmentStatus(file)"
              :aria-label="`附件状态 · ${file.name || ''}`"
              @change="setAttachmentStatus(file, $event.target.value, {
                taskId: filesModalTask?.taskId,
                task: filesModalTask,
              })"
            >
              <option v-for="opt in ATTACH_STATUS" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
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
      :title="projectIsEdit ? '编辑父任务' : '新建父任务'"
      :columns="2"
      :loading="projectSubmitting"
      :confirm-text="projectIsEdit ? '保存修改' : '确认创建'"
      @confirm="submitProjectForm"
    >
      <a-form layout="vertical" :model="projectForm" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="标题" class="dialog-item dialog-item--full" required>
            <a-input v-model:value.trim="projectForm.title" placeholder="例如：个人知识库改版" :maxlength="100" />
          </a-form-item>

          <a-form-item label="归档状态" class="dialog-item">
            <a-select v-model:value="projectForm.archived" style="width: 100%">
              <a-select-option value="0">进行中</a-select-option>
              <a-select-option value="1">已归档</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="显示顺序" class="dialog-item">
            <a-input-number v-model:value="projectForm.displayOrder" :min="0" style="width: 100%" placeholder="越小越靠前" />
          </a-form-item>

          <a-form-item label="描述" class="dialog-item dialog-item--full">
            <a-textarea v-model:value.trim="projectForm.description" :rows="3" placeholder="一句话说明要做什么" :maxlength="500" />
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
            必须指定整组截止时间：两周与两个月会按不同节奏拆阶段；预览确认后再写入看板。
          </p>
          <a-form layout="vertical" class="dialog-form">
            <a-form-item label="截止时间" required>
              <a-date-picker
                v-model:value="planForm.deadline"
                format="YYYY-MM-DD"
                style="width: 100%"
                placeholder="整组要在哪天完成"
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
                placeholder="要达成什么？默认使用描述"
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
.tp-charts {
  display: grid;
  grid-template-columns: minmax(180px, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
  margin-bottom: var(--space-4);
}

.tp-chart-card {
  position: relative;
  overflow: hidden;
  padding: 12px 14px 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs), inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
}

.tp-chart-card--gauge {
  background: linear-gradient(145deg, rgba(8, 145, 178, 0.12) 0%, rgba(255, 255, 255, 0.9) 68%);
  border-color: rgba(8, 145, 178, 0.2);
}

.tp-chart {
  width: 100%;
  height: 168px;
}

.tp-chart-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin: 0 4px 2px;
  padding-top: 6px;
  border-top: 1px solid rgba(8, 145, 178, 0.08);
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

.proj-progress {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.proj-progress__chart {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.proj-progress__pct {
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  color: var(--color-accent-deep);
}

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

.project-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 var(--space-4);
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid var(--color-border-light);
  background: var(--tp-glass);
  backdrop-filter: blur(8px);
}

.project-strip__label {
  flex-shrink: 0;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-dim);
}

.project-strip__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.project-strip__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 220px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(8, 145, 178, 0.18);
  background: var(--color-accent-soft);
  color: var(--color-accent-deep);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.project-strip__chip:hover {
  transform: translateY(-1px);
  border-color: rgba(8, 145, 178, 0.35);
  box-shadow: var(--shadow-xs);
}

.wb-empty__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 16px;
}

.task-view__toolbar {
  margin-bottom: var(--space-4);
}

.task-view__prio {
  width: 120px;
}

.task-view__binding {
  width: 160px;
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

.meta-chip--phase {
  color: #0f766e;
  background: rgba(13, 148, 136, 0.1);
  border-color: rgba(13, 148, 136, 0.18);
}

/* —— 任务组阶段树 —— */
.phase-tree {
  margin-bottom: var(--space-4);
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.phase-tree__title {
  margin: 0 0 10px;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--color-text-secondary);
}

.phase-tree__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.phase-tree__phase {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(8, 145, 178, 0.04);
  border: 1px solid rgba(8, 145, 178, 0.1);
}

.phase-tree__phase-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.phase-tree__phase-name {
  font-weight: 650;
  font-size: 0.92rem;
  color: var(--tp-ink);
}

.phase-tree__phase-count {
  font-size: 0.72rem;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.phase-tree__edit {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
}

.phase-tree__edit:hover {
  color: var(--color-accent-deep);
  background: rgba(8, 145, 178, 0.1);
}

.phase-tree__steps {
  list-style: none;
  margin: 0;
  padding: 0 0 0 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.phase-tree__step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.phase-tree__step-status {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #94a3b8;
}

.phase-tree__step-status[data-s='1'] {
  background: #0891b2;
}

.phase-tree__step-status[data-s='2'] {
  background: #16a34a;
}

.phase-tree__step-status[data-s='3'] {
  background: #94a3b8;
  opacity: 0.55;
}

.phase-tree__step-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phase-tree__empty {
  margin: 0;
  padding-left: 8px;
  font-size: 0.78rem;
  color: var(--color-text-tertiary);
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

.attach-review__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.attach-review__item.is-status-2,
.files-modal__item.is-status-2 {
  opacity: 0.9;
}

.attach-badge {
  flex-shrink: 0;
  font-size: 0.68rem;
  font-weight: 650;
  letter-spacing: 0.02em;
  line-height: 1;
  padding: 3px 6px;
  border-radius: 4px;
  border: 1px solid transparent;
}

.attach-badge--todo {
  color: #9a5b16;
  background: rgba(214, 148, 48, 0.12);
  border-color: rgba(214, 148, 48, 0.28);
}

.attach-badge--doing {
  color: #1f5f8b;
  background: rgba(47, 128, 186, 0.12);
  border-color: rgba(47, 128, 186, 0.28);
}

.attach-badge--done {
  color: #2f6b45;
  background: rgba(56, 142, 90, 0.12);
  border-color: rgba(56, 142, 90, 0.28);
}

.attach-status-select {
  max-width: 7.5rem;
  height: 26px;
  padding: 0 6px;
  border: 1px solid var(--color-border-light);
  border-radius: 6px;
  background: #fff;
  font: inherit;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
}

.attach-status-select:focus {
  outline: 2px solid rgba(47, 128, 186, 0.28);
  outline-offset: 1px;
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

  .tp-charts {
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

