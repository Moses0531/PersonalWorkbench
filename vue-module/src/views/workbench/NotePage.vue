<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addNoteApi,
  archiveNoteApi,
  deleteNotesApi,
  pageNotesApi,
  updateNoteApi,
} from '@/apis/workbench/NoteApi'
import {
  noteConsolidateApi,
  noteToTasksApplyApi,
  noteToTasksPreviewApi,
} from '@/apis/ai/AiApi'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import {
  PlusOutlined,
  ThunderboltOutlined,
  ProjectOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const consolidating = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const notes = ref([])
const searchQuery = ref('')
const filterType = ref('all')
const selectedIds = ref([])
const captureTitle = ref('')
const captureContent = ref('')
const capturing = ref(false)

const consolidateOpen = ref(false)
const consolidateForm = reactive({
  title: '',
  instruction: '',
})

const toTasksOpen = ref(false)
const toTasksNoteId = ref(null)
const toTasksPreviewing = ref(false)
const toTasksApplying = ref(false)
const toTasksPreview = ref(null)
const toTasksForm = reactive({
  deadline: null,
  goal: '',
  constraints: '',
})

const form = reactive({
  noteId: null,
  title: '',
  content: '',
  noteType: 'scrap',
  status: '0',
  sourceNoteIds: null,
  linkedTaskId: null,
  remark: '',
})

let searchTimer = null

const stats = computed(() => {
  const all = notes.value
  const scraps = all.filter((n) => n.noteType === 'scrap').length
  const digests = all.filter((n) => n.noteType === 'digest').length
  return { total: all.length, scraps, digests }
})

const selectedScraps = computed(() =>
  notes.value.filter(
    (n) => selectedIds.value.includes(n.noteId) && n.noteType === 'scrap',
  ),
)

const sourceIdsOfCurrent = computed(() => parseSourceIds(form.sourceNoteIds))

watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadNotes(), 320)
})

watch(filterType, () => {
  selectedIds.value = []
  loadNotes()
})

function parseSourceIds(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.map(Number).filter(Boolean)
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed.map(Number).filter(Boolean) : []
    } catch {
      return []
    }
  }
  return []
}

function formatTime(value) {
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-'
}

function typeLabel(row) {
  return row?.noteType === 'digest' ? '整理稿' : '碎片'
}

function contentPreview(raw) {
  if (!raw) return ''
  return String(raw)
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/(\*\*|__)(.*?)\1/g, '$2')
    .replace(/(\*|_)(.*?)\1/g, '$2')
    .replace(/^[-*+]\s+/gm, '')
    .replace(/^\d+\.\s+/gm, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function listParams() {
  const params = { pageNum: 1, pageRows: 200, keyword: searchQuery.value.trim() || undefined }
  if (filterType.value === 'scrap') {
    params.noteType = 'scrap'
    params.status = '0'
  } else if (filterType.value === 'digest') {
    params.noteType = 'digest'
    params.status = '0'
  } else if (filterType.value === 'archived') {
    params.status = '1'
  } else {
    params.status = '0'
  }
  return params
}

async function loadNotes() {
  loading.value = true
  try {
    const result = await pageNotesApi(listParams())
    notes.value = result?.data?.records || []
    const alive = new Set(notes.value.map((n) => n.noteId))
    selectedIds.value = selectedIds.value.filter((id) => alive.has(id))
  } catch (error) {
    message.error(error.message || '获取笔记列表失败')
  } finally {
    loading.value = false
  }
}

async function quickCapture() {
  const content = captureContent.value.trim()
  if (!content) {
    message.warning('先写点什么再保存')
    return
  }
  capturing.value = true
  try {
    await addNoteApi({
      title: captureTitle.value.trim() || undefined,
      content,
      noteType: 'scrap',
    })
    captureTitle.value = ''
    captureContent.value = ''
    message.success('已记下一笔')
    await loadNotes()
  } catch (error) {
    message.error(error.message || '保存失败')
  } finally {
    capturing.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    noteId: null,
    title: '',
    content: '',
    noteType: 'scrap',
    status: '0',
    sourceNoteIds: null,
    linkedTaskId: null,
    remark: '',
  })
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    noteId: row.noteId,
    title: row.title || '',
    content: row.content || '',
    noteType: row.noteType || 'scrap',
    status: row.status || '0',
    sourceNoteIds: row.sourceNoteIds,
    linkedTaskId: row.linkedTaskId || null,
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

function openSourceNote(id) {
  const row = notes.value.find((n) => n.noteId === id)
  if (row) {
    openEdit(row)
    return
  }
  message.info('来源碎片不在当前列表，可切换筛选后查看')
}

async function submitForm() {
  if (!form.title.trim() && !form.content.trim()) {
    message.warning('请填写标题或正文')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateNoteApi({
        noteId: form.noteId,
        title: form.title.trim(),
        content: form.content,
        remark: form.remark,
        status: form.status,
      })
      message.success('已保存')
    } else {
      await addNoteApi({
        title: form.title.trim() || undefined,
        content: form.content,
        noteType: 'scrap',
        remark: form.remark,
      })
      message.success('已创建')
    }
    dialogVisible.value = false
    await loadNotes()
  } catch (error) {
    message.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function removeNote(id) {
  try {
    await deleteNotesApi([id])
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
    message.success('已删除')
    await loadNotes()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function removeSelected() {
  if (!selectedIds.value.length) return
  try {
    await deleteNotesApi(selectedIds.value)
    selectedIds.value = []
    message.success('已批量删除')
    await loadNotes()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function toggleArchive(row) {
  const next = String(row.status) === '1' ? '0' : '1'
  try {
    await archiveNoteApi(row.noteId, next)
    message.success(next === '1' ? '已归档' : '已恢复')
    await loadNotes()
  } catch (error) {
    message.error(error.message || '操作失败')
  }
}

function toggleSelect(id, checked) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value = [...selectedIds.value, id]
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  }
}

function openConsolidate() {
  if (selectedScraps.value.length < 1) {
    message.warning('请先勾选至少一条碎片')
    return
  }
  consolidateForm.title = ''
  consolidateForm.instruction = ''
  consolidateOpen.value = true
}

async function runConsolidate() {
  consolidating.value = true
  try {
    const result = await noteConsolidateApi({
      noteIds: selectedScraps.value.map((n) => n.noteId),
      title: consolidateForm.title.trim() || undefined,
      instruction: consolidateForm.instruction.trim() || undefined,
    })
    consolidateOpen.value = false
    selectedIds.value = []
    message.success('整合完成')
    await loadNotes()
    const note = result?.data?.note
    if (note) {
      openEdit(note)
    }
  } catch (error) {
    message.error(error.message || 'AI 整合失败')
  } finally {
    consolidating.value = false
  }
}

function openToTasks(row) {
  if (row?.noteType !== 'digest') {
    message.warning('仅整理稿可生成待办')
    return
  }
  toTasksNoteId.value = row.noteId
  toTasksPreview.value = null
  Object.assign(toTasksForm, {
    deadline: dayjs().add(14, 'day'),
    goal: '',
    constraints: '',
  })
  toTasksOpen.value = true
}

async function runToTasksPreview() {
  if (!toTasksForm.deadline) {
    message.warning('请选择截止时间')
    return
  }
  toTasksPreviewing.value = true
  try {
    const result = await noteToTasksPreviewApi({
      noteId: toTasksNoteId.value,
      deadline: toTasksForm.deadline.format('YYYY-MM-DD'),
      goal: toTasksForm.goal.trim() || undefined,
      constraints: toTasksForm.constraints.trim() || undefined,
    })
    toTasksPreview.value = result?.data || null
    if (!toTasksPreview.value?.phases?.length) {
      message.warning('未生成有效阶段')
    }
  } catch (error) {
    message.error(error.message || 'AI 规划失败')
  } finally {
    toTasksPreviewing.value = false
  }
}

async function applyToTasks() {
  if (!toTasksPreview.value?.planBatchId || !toTasksPreview.value?.phases?.length) {
    message.warning('请先生成预览')
    return
  }
  toTasksApplying.value = true
  try {
    const result = await noteToTasksApplyApi({
      noteId: toTasksNoteId.value,
      planBatchId: toTasksPreview.value.planBatchId,
      phases: toTasksPreview.value.phases,
      deadline: toTasksPreview.value.deadline,
    })
    toTasksOpen.value = false
    message.success(`已生成 ${result?.data?.created ?? ''} 条待办`)
    await loadNotes()
    const taskId = result?.data?.taskId || result?.data?.projectTaskId
    if (taskId) {
      router.push({ path: '/task', query: { projectTaskId: String(taskId) } })
    }
  } catch (error) {
    message.error(error.message || '落板失败')
  } finally {
    toTasksApplying.value = false
  }
}

function goLinkedTask(taskId) {
  if (!taskId) return
  router.push({ path: '/task', query: { projectTaskId: String(taskId) } })
}

onMounted(() => {
  loadNotes()
})
</script>

<template>
  <div class="wb-page note-page">
    <div class="wb-page__blob wb-page__blob--1" aria-hidden="true" />
    <div class="wb-page__blob wb-page__blob--2" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header">
        <div class="wb-header__text">
          <h1 class="wb-header__title">笔记</h1>
          <p class="wb-header__desc">随手记下零散想法，勾选后交给 AI 整理，需要时再落成待办。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="loadNotes">
            刷新
          </button>
          <button v-permission="'note:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate">
            <PlusOutlined />
            新建
          </button>
        </div>
      </header>

      <div class="wb-stats">
        <div class="wb-stat wb-stat--accent">
          <span class="wb-stat__label">当前列表</span>
          <span class="wb-stat__value">{{ stats.total }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">碎片</span>
          <span class="wb-stat__value">{{ stats.scraps }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">整理稿</span>
          <span class="wb-stat__value">{{ stats.digests }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">已勾选</span>
          <span class="wb-stat__value">{{ selectedScraps.length }}</span>
        </div>
      </div>

      <section v-permission="'note:add'" class="capture-box" aria-label="快速录入">
        <input
          v-model="captureTitle"
          type="text"
          class="capture-box__title"
          maxlength="200"
          placeholder="标题（可选）"
        />
        <textarea
          v-model="captureContent"
          class="capture-box__content"
          rows="3"
          placeholder="随手记下一句想法、待办线索或摘录…"
        />
        <div class="capture-box__bar">
          <button
            type="button"
            class="wb-btn wb-btn--primary"
            :disabled="capturing || !captureContent.trim()"
            @click="quickCapture"
          >
            {{ capturing ? '保存中…' : '记下' }}
          </button>
        </div>
      </section>

      <div class="wb-toolbar">
        <input
          v-model="searchQuery"
          type="search"
          class="wb-search"
          placeholder="搜索标题或正文关键词（空格分词）…"
          aria-label="搜索笔记"
        />
        <div class="filter-tabs" role="tablist" aria-label="笔记类型">
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterType === 'all' }"
            @click="filterType = 'all'"
          >
            全部
          </button>
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterType === 'scrap' }"
            @click="filterType = 'scrap'"
          >
            碎片
          </button>
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterType === 'digest' }"
            @click="filterType = 'digest'"
          >
            整理稿
          </button>
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterType === 'archived' }"
            @click="filterType = 'archived'"
          >
            已归档
          </button>
        </div>
        <div class="toolbar-actions">
          <button
            v-permission="'ai:note:consolidate'"
            type="button"
            class="wb-btn wb-btn--primary"
            :disabled="!selectedScraps.length || consolidating"
            @click="openConsolidate"
          >
            <ThunderboltOutlined />
            AI 整合
          </button>
          <a-popconfirm
            v-if="selectedIds.length"
            v-permission="'note:remove'"
            title="确认删除勾选的笔记？"
            @confirm="removeSelected"
          >
            <button type="button" class="wb-btn wb-btn--ghost is-danger">批量删除</button>
          </a-popconfirm>
        </div>
      </div>

      <a-spin :spinning="loading">
        <div v-if="!notes.length" class="empty-panel">
          <p class="empty-panel__title">还没有笔记</p>
          <p class="empty-panel__desc">先快速记几条碎片，再勾选交给 AI 整合成整理稿，需要时落成待办。</p>
        </div>

        <ul v-else class="note-list" aria-label="笔记列表">
          <li v-for="row in notes" :key="row.noteId" class="note-card">
            <div class="note-card__check" v-if="row.noteType === 'scrap' && filterType !== 'archived'">
              <a-checkbox
                :checked="selectedIds.includes(row.noteId)"
                @change="(e) => toggleSelect(row.noteId, e.target.checked)"
              />
            </div>
            <div class="note-card__main">
              <div class="note-card__meta">
                <time class="note-card__time">{{ formatTime(row.updateTime) }}</time>
                <span class="wb-chip" :class="row.noteType === 'digest' ? 'wb-chip--active' : ''">
                  {{ typeLabel(row) }}
                </span>
                <span v-if="String(row.status) === '1'" class="note-card__tag">已归档</span>
                <span v-if="row.linkedTaskId" class="note-card__tag">已落待办</span>
              </div>
              <h2 class="note-card__title">{{ row.title || '未命名' }}</h2>
              <p v-if="contentPreview(row.content)" class="note-card__summary">
                {{ contentPreview(row.content) }}
              </p>
            </div>
            <div class="note-card__actions">
              <button
                v-permission="'note:modify'"
                type="button"
                class="wb-btn wb-btn--ghost"
                @click="openEdit(row)"
              >
                打开
              </button>
              <button
                v-if="row.noteType === 'digest'"
                v-permission="'ai:note:to-tasks'"
                type="button"
                class="wb-btn wb-btn--ghost"
                :disabled="!!row.linkedTaskId"
                @click="openToTasks(row)"
              >
                <ProjectOutlined />
                {{ row.linkedTaskId ? '已生成' : '生成待办' }}
              </button>
              <button
                v-if="row.linkedTaskId"
                type="button"
                class="wb-btn wb-btn--ghost"
                @click="goLinkedTask(row.linkedTaskId)"
              >
                查看待办
              </button>
              <button
                v-permission="'note:modify'"
                type="button"
                class="wb-btn wb-btn--ghost"
                @click="toggleArchive(row)"
              >
                {{ String(row.status) === '1' ? '恢复' : '归档' }}
              </button>
              <a-popconfirm
                v-permission="'note:remove'"
                title="确认删除该笔记？"
                @confirm="removeNote(row.noteId)"
              >
                <button type="button" class="wb-btn wb-btn--ghost is-danger">删除</button>
              </a-popconfirm>
            </div>
          </li>
        </ul>
      </a-spin>
    </div>

    <DataOperationView
      v-model="dialogVisible"
      :title="isEdit ? (form.noteType === 'digest' ? '整理稿详情' : '笔记详情') : '新建碎片'"
      :columns="1"
      :loading="submitting"
      :confirm-text="isEdit ? '保存修改' : '创建'"
      @confirm="submitForm"
    >
      <a-form layout="vertical" :model="form" class="dialog-form">
        <a-form-item label="标题">
          <a-input v-model:value.trim="form.title" placeholder="可选，留空则自动截取正文" :maxlength="200" />
        </a-form-item>
        <a-form-item label="正文" required>
          <a-textarea
            v-model:value="form.content"
            :rows="12"
            placeholder="支持 Markdown"
          />
        </a-form-item>
        <a-form-item v-if="form.noteType === 'digest' && sourceIdsOfCurrent.length" label="来源碎片">
          <div class="source-ids">
            <button
              v-for="id in sourceIdsOfCurrent"
              :key="id"
              type="button"
              class="source-id"
              @click="openSourceNote(id)"
            >
              #{{ id }}
            </button>
          </div>
        </a-form-item>
        <a-form-item v-if="form.linkedTaskId" label="关联待办">
          <button type="button" class="wb-btn wb-btn--ghost" @click="goLinkedTask(form.linkedTaskId)">
            打开待办 #{{ form.linkedTaskId }}
          </button>
        </a-form-item>
        <a-form-item
          v-if="isEdit && form.noteType === 'digest'"
          label="生成待办"
        >
          <button
            v-permission="'ai:note:to-tasks'"
            type="button"
            class="wb-btn wb-btn--primary"
            :disabled="!!form.linkedTaskId"
            @click="openToTasks(form)"
          >
            <ProjectOutlined />
            {{ form.linkedTaskId ? '已生成待办' : 'AI 生成待办' }}
          </button>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value.trim="form.remark" placeholder="选填" />
        </a-form-item>
      </a-form>
    </DataOperationView>

    <a-modal
      v-model:open="consolidateOpen"
      title="AI 整合碎片"
      :confirm-loading="consolidating"
      ok-text="开始整合"
      cancel-text="取消"
      :mask-closable="false"
      @ok="runConsolidate"
    >
      <p class="modal-hint">
        已选 {{ selectedScraps.length }} 条碎片，将生成一篇整理稿，原碎片保留可回溯。
      </p>
      <a-form layout="vertical">
        <a-form-item label="整理稿标题（可选）">
          <a-input v-model:value.trim="consolidateForm.title" :maxlength="200" placeholder="不填则由 AI 拟定" />
        </a-form-item>
        <a-form-item label="补充说明（可选）">
          <a-textarea
            v-model:value="consolidateForm.instruction"
            :rows="3"
            :maxlength="500"
            show-count
            placeholder="例如：侧重待办、合并重复观点…"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="toTasksOpen"
      title="整理稿生成待办"
      width="640px"
      :mask-closable="false"
      :confirm-loading="toTasksApplying"
      :ok-button-props="{ disabled: !toTasksPreview?.phases?.length || toTasksPreviewing }"
      ok-text="确认落板"
      cancel-text="关闭"
      @ok="applyToTasks"
    >
      <a-spin :spinning="toTasksPreviewing">
        <p class="modal-hint">指定截止日后生成阶段预览，确认后再写入事务看板。</p>
        <a-form layout="vertical">
          <a-form-item label="截止时间" required>
            <a-date-picker
              v-model:value="toTasksForm.deadline"
              format="YYYY-MM-DD"
              style="width: 100%"
              :disabled-date="(current) => current && current < dayjs().startOf('day')"
            />
          </a-form-item>
          <a-form-item label="规划目标">
            <a-textarea v-model:value="toTasksForm.goal" :rows="2" :maxlength="2000" show-count />
          </a-form-item>
          <a-form-item label="其它约束（可选）">
            <a-textarea v-model:value="toTasksForm.constraints" :rows="2" :maxlength="1000" show-count />
          </a-form-item>
        </a-form>
        <div class="plan-actions">
          <button
            type="button"
            class="wb-btn wb-btn--primary"
            :disabled="toTasksPreviewing || !toTasksForm.deadline"
            @click="runToTasksPreview"
          >
            <ThunderboltOutlined />
            {{ toTasksPreview ? '重新生成' : '生成预览' }}
          </button>
        </div>
        <div v-if="toTasksPreview" class="plan-preview">
          <p v-if="toTasksPreview.daysLeft != null" class="plan-preview__meta">
            截止 {{ toTasksPreview.deadline }} · 剩 {{ toTasksPreview.daysLeft }} 天
          </p>
          <p v-if="toTasksPreview.summary" class="plan-preview__summary">{{ toTasksPreview.summary }}</p>
          <ol class="plan-preview__phases">
            <li
              v-for="(phase, pi) in toTasksPreview.phases"
              :key="`${pi}-${phase.title}`"
              class="plan-preview__phase"
            >
              <div class="plan-preview__phase-title">
                {{ phase.title }}
                <span v-if="phase.dueTime" class="plan-preview__due">{{ phase.dueTime }}</span>
              </div>
              <p v-if="phase.description" class="plan-preview__phase-desc">{{ phase.description }}</p>
              <ul v-if="phase.steps?.length" class="plan-preview__steps">
                <li v-for="(step, si) in phase.steps" :key="`${si}-${step.title}`">
                  {{ step.title }}
                  <span v-if="step.dueTime" class="plan-preview__due">{{ step.dueTime }}</span>
                </li>
              </ul>
            </li>
          </ol>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.capture-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.capture-box__title {
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
  background: rgba(238, 248, 252, 0.45);
  font-size: 0.9rem;
  font-weight: 600;
}

.capture-box__content {
  resize: vertical;
  min-height: 72px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
  background: rgba(238, 248, 252, 0.35);
  font-size: 0.9rem;
  line-height: 1.55;
  font-family: inherit;
}

.capture-box__bar {
  display: flex;
  justify-content: flex-end;
}

.wb-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

.filter-tabs {
  display: inline-flex;
  gap: 6px;
  padding: 4px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--color-border-light);
}

.filter-tab {
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.82rem;
  font-weight: 650;
  cursor: pointer;
}

.filter-tab.is-active {
  background: rgba(8, 145, 178, 0.12);
  color: var(--color-accent-deep);
}

.empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 240px;
  padding: var(--space-6);
  text-align: center;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--color-border-light);
}

.empty-panel__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
}

.empty-panel__desc {
  margin: 0;
  max-width: 40ch;
  font-size: 0.88rem;
  line-height: 1.55;
  color: var(--color-text-secondary);
}

.note-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.note-card__check {
  padding-top: 4px;
}

.note-card__main {
  min-width: 0;
  flex: 1;
}

.note-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.note-card__time {
  font-size: 0.8rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-accent-deep);
}

.note-card__tag {
  font-size: 0.72rem;
  font-weight: 650;
  color: var(--color-text-dim);
}

.note-card__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.35;
}

.note-card__summary {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(238, 248, 252, 0.55);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
  font-size: 0.82rem;
  line-height: 1.6;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-card__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
  max-width: 280px;
}

.is-danger:hover {
  color: var(--color-red);
  border-color: rgba(224, 85, 69, 0.35);
}

.source-ids {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-id {
  border: 1px solid var(--color-border-light);
  background: rgba(8, 145, 178, 0.08);
  color: var(--color-accent-deep);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}

.modal-hint {
  margin: 0 0 12px;
  font-size: 0.86rem;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.plan-actions {
  margin: 8px 0 14px;
}

.plan-preview {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  border-radius: 12px;
  background: rgba(238, 248, 252, 0.55);
  border: 1px solid var(--color-border-light);
}

.plan-preview__meta {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-accent-deep);
}

.plan-preview__summary {
  margin: 0;
  font-size: 0.86rem;
  line-height: 1.5;
}

.plan-preview__phases {
  margin: 0;
  padding-left: 18px;
}

.plan-preview__phase {
  margin-bottom: 10px;
}

.plan-preview__phase-title {
  font-weight: 700;
  font-size: 0.9rem;
}

.plan-preview__phase-desc {
  margin: 4px 0;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
}

.plan-preview__steps {
  margin: 4px 0 0;
  padding-left: 16px;
  font-size: 0.8rem;
  color: var(--color-text-body);
}

.plan-preview__due {
  margin-left: 8px;
  font-size: 0.72rem;
  font-weight: 650;
  color: var(--color-text-dim);
}

@media (max-width: 900px) {
  .note-card {
    flex-direction: column;
  }

  .note-card__actions {
    max-width: none;
    width: 100%;
  }

  .toolbar-actions {
    margin-left: 0;
    width: 100%;
  }
}
</style>
