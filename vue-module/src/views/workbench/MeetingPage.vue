<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addMeetingApi,
  deleteMeetingsApi,
  pageMeetingsApi,
  removeMeetingAttachmentApi,
  updateMeetingApi,
  uploadMeetingAttachmentApi,
} from '@/apis/workbench/MeetingApi'
import { meetingSummaryApi } from '@/apis/ai/AiApi'
import DataOperationView from '@/components/ListView/DataOperationView.vue'
import { PlusOutlined, UploadOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const summarizing = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const meetings = ref([])
const searchQuery = ref('')
const filterStatus = ref('all')
const currentAttachments = ref([])
const currentSummary = ref('')

const form = reactive({
  meetingId: null,
  title: '',
  meetingTime: null,
  location: '',
  participants: '',
  remark: '',
})

const stats = computed(() => {
  const all = meetings.value
  const done = all.filter((m) => String(m.status) === '1').length
  const pending = all.length - done
  const withFiles = all.filter((m) => parseAttachments(m.attachments).length > 0).length
  return { total: all.length, pending, done, withFiles }
})

const filteredMeetings = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  return meetings.value
    .filter((m) => {
      if (filterStatus.value === '0' && String(m.status) !== '0') return false
      if (filterStatus.value === '1' && String(m.status) !== '1') return false
      if (!q) return true
      return (
        (m.title || '').toLowerCase().includes(q) ||
        (m.location || '').toLowerCase().includes(q) ||
        (m.participants || '').toLowerCase().includes(q) ||
        (m.aiSummary || '').toLowerCase().includes(q)
      )
    })
    .slice()
    .sort((a, b) => dayjs(b.meetingTime).valueOf() - dayjs(a.meetingTime).valueOf())
})

function parseAttachments(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
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

function formatSize(size) {
  const n = Number(size) || 0
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

function statusLabel(row) {
  return String(row?.status) === '1' ? '已整理' : '待整理'
}

function attachmentCount(row) {
  return parseAttachments(row?.attachments).length
}

function resetForm() {
  Object.assign(form, {
    meetingId: null,
    title: '',
    meetingTime: dayjs(),
    location: '',
    participants: '',
    remark: '',
  })
  currentAttachments.value = []
  currentSummary.value = ''
}

async function loadMeetings() {
  loading.value = true
  try {
    const result = await pageMeetingsApi(1, null)
    meetings.value = result?.data?.records || []
  } catch (error) {
    message.error(error.message || '获取会议列表失败')
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
    meetingId: row.meetingId,
    title: row.title || '',
    meetingTime: row.meetingTime ? dayjs(row.meetingTime) : dayjs(),
    location: row.location || '',
    participants: row.participants || '',
    remark: row.remark || '',
  })
  currentAttachments.value = parseAttachments(row.attachments)
  currentSummary.value = row.aiSummary || ''
  dialogVisible.value = true
}

function toPayload() {
  const t = form.meetingTime ? dayjs(form.meetingTime) : null
  return {
    meetingId: form.meetingId,
    title: form.title.trim(),
    meetingTime: t?.isValid() ? t.format('YYYY-MM-DD HH:mm:ss') : null,
    location: form.location?.trim() || '',
    participants: form.participants?.trim() || '',
    remark: form.remark?.trim() || '',
  }
}

async function submitForm() {
  if (!form.title?.trim()) {
    message.warning('请填写会议标题')
    return
  }
  if (!form.meetingTime) {
    message.warning('请选择会议时间')
    return
  }
  submitting.value = true
  try {
    const payload = toPayload()
    if (isEdit.value) {
      await updateMeetingApi(payload)
      message.success('已保存')
      dialogVisible.value = false
      await loadMeetings()
    } else {
      const result = await addMeetingApi(payload)
      const created = result?.data
      message.success('已创建，可继续上传材料')
      if (created?.meetingId) {
        isEdit.value = true
        form.meetingId = created.meetingId
        currentAttachments.value = []
        currentSummary.value = ''
      } else {
        dialogVisible.value = false
      }
      await loadMeetings()
    }
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function removeMeeting(id) {
  try {
    await deleteMeetingsApi(id)
    message.success('删除成功')
    if (form.meetingId === id) dialogVisible.value = false
    await loadMeetings()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function onUpload({ file, onSuccess, onError }) {
  if (!form.meetingId) {
    message.warning('请先创建会议，再上传材料')
    onError?.(new Error('meeting not created'))
    return
  }
  uploading.value = true
  try {
    const result = await uploadMeetingAttachmentApi(form.meetingId, file)
    const item = result?.data
    if (item) currentAttachments.value = [...currentAttachments.value, item]
    message.success('材料已上传')
    onSuccess?.(item)
    await loadMeetings()
    const latest = meetings.value.find((m) => Number(m.meetingId) === Number(form.meetingId))
    if (latest) currentAttachments.value = parseAttachments(latest.attachments)
  } catch (error) {
    message.error(error.message || '上传失败')
    onError?.(error)
  } finally {
    uploading.value = false
  }
}

async function removeAttachment(attachmentId) {
  if (!form.meetingId || !attachmentId) return
  try {
    await removeMeetingAttachmentApi(form.meetingId, attachmentId)
    currentAttachments.value = currentAttachments.value.filter(
      (f) => String(f.id) !== String(attachmentId),
    )
    message.success('已删除材料')
    await loadMeetings()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

async function runAiSummary() {
  if (!form.meetingId) {
    message.warning('请先创建会议')
    return
  }
  if (!currentAttachments.value.length) {
    message.warning('请先上传会议材料')
    return
  }
  summarizing.value = true
  try {
    const result = await meetingSummaryApi(form.meetingId)
    currentSummary.value = result?.data?.aiSummary || ''
    message.success('AI 整理完成')
    await loadMeetings()
  } catch (error) {
    message.error(error.message || 'AI 整理失败')
  } finally {
    summarizing.value = false
  }
}

onMounted(loadMeetings)
</script>

<template>
  <div class="wb-page meeting-page">
    <div class="wb-page__blob wb-page__blob--1" aria-hidden="true" />
    <div class="wb-page__blob wb-page__blob--2" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header">
        <div class="wb-header__text">
          <h1 class="wb-header__title">会议记录</h1>
          <p class="wb-header__desc">上传会议材料，一键 AI 整理成概要，少打字多沉淀。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="loadMeetings">
            刷新
          </button>
          <button v-permission="'meeting:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate">
            <PlusOutlined />
            新建会议
          </button>
        </div>
      </header>

      <div class="wb-stats">
        <div class="wb-stat wb-stat--accent">
          <span class="wb-stat__label">全部会议</span>
          <span class="wb-stat__value">{{ stats.total }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">待整理</span>
          <span class="wb-stat__value">{{ stats.pending }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">已整理</span>
          <span class="wb-stat__value">{{ stats.done }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">有材料</span>
          <span class="wb-stat__value">{{ stats.withFiles }}</span>
        </div>
      </div>

      <div class="wb-toolbar">
        <input
          v-model="searchQuery"
          type="search"
          class="wb-search"
          placeholder="搜索标题、地点、参会人、概要..."
          aria-label="搜索会议"
        />
        <div class="filter-tabs" role="tablist" aria-label="整理状态">
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterStatus === 'all' }"
            @click="filterStatus = 'all'"
          >
            全部
          </button>
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterStatus === '0' }"
            @click="filterStatus = '0'"
          >
            待整理
          </button>
          <button
            type="button"
            class="filter-tab"
            :class="{ 'is-active': filterStatus === '1' }"
            @click="filterStatus = '1'"
          >
            已整理
          </button>
        </div>
      </div>

      <a-spin :spinning="loading">
        <div v-if="!filteredMeetings.length" class="empty-panel">
          <p class="empty-panel__title">还没有会议记录</p>
          <p class="empty-panel__desc">新建一场会议，上传草稿或纪要文件，让 AI 帮你整理框架。</p>
          <button v-permission="'meeting:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate">
            新建会议
          </button>
        </div>

        <ul v-else class="meeting-list" aria-label="会议列表">
          <li v-for="row in filteredMeetings" :key="row.meetingId" class="meeting-card">
            <div class="meeting-card__main">
              <div class="meeting-card__meta">
                <time class="meeting-card__time">{{ formatTime(row.meetingTime) }}</time>
                <span class="wb-chip" :class="String(row.status) === '1' ? 'wb-chip--active' : ''">
                  {{ statusLabel(row) }}
                </span>
                <span v-if="attachmentCount(row)" class="meeting-card__files">
                  {{ attachmentCount(row) }} 份材料
                </span>
              </div>
              <h2 class="meeting-card__title">{{ row.title }}</h2>
              <p v-if="row.location || row.participants" class="meeting-card__sub">
                <span v-if="row.location">{{ row.location }}</span>
                <span v-if="row.location && row.participants"> · </span>
                <span v-if="row.participants">{{ row.participants }}</span>
              </p>
              <p v-if="row.aiSummary" class="meeting-card__summary">{{ row.aiSummary }}</p>
            </div>
            <div class="meeting-card__actions">
              <button
                v-permission="'meeting:modify'"
                type="button"
                class="wb-btn wb-btn--ghost"
                @click="openEdit(row)"
              >
                打开
              </button>
              <a-popconfirm
                v-permission="'meeting:remove'"
                title="确认删除该会议记录？"
                @confirm="removeMeeting(row.meetingId)"
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
      :title="isEdit ? '会议详情' : '新建会议'"
      :columns="2"
      :loading="submitting"
      :confirm-text="isEdit ? '保存修改' : '创建并继续'"
      @confirm="submitForm"
    >
      <a-form layout="vertical" :model="form" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="标题" class="dialog-item dialog-item--full" required>
            <a-input
              v-model:value.trim="form.title"
              placeholder="例如：产品研讨会"
              :maxlength="120"
            />
          </a-form-item>

          <a-form-item label="会议时间" class="dialog-item" required>
            <a-date-picker
              v-model:value="form.meetingTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item label="地点" class="dialog-item">
            <a-input v-model:value.trim="form.location" placeholder="选填" />
          </a-form-item>

          <a-form-item label="参会人" class="dialog-item dialog-item--full">
            <a-input v-model:value.trim="form.participants" placeholder="选填，逗号分隔" />
          </a-form-item>

          <a-form-item label="备注" class="dialog-item dialog-item--full">
            <a-input v-model:value.trim="form.remark" placeholder="选填" />
          </a-form-item>

          <a-form-item label="会议材料" class="dialog-item dialog-item--full">
            <div class="attach-box">
              <p v-if="!isEdit || !form.meetingId" class="attach-box__hint">
                先创建会议，再上传草稿 / 纪要等材料
              </p>
              <template v-else>
                <a-upload
                  v-permission="'meeting:modify'"
                  :custom-request="onUpload"
                  :show-upload-list="false"
                  :disabled="uploading"
                >
                  <button type="button" class="wb-btn wb-btn--ghost" :disabled="uploading">
                    <UploadOutlined />
                    {{ uploading ? '上传中…' : '上传材料' }}
                  </button>
                </a-upload>

                <ul v-if="currentAttachments.length" class="attach-list">
                  <li v-for="file in currentAttachments" :key="file.id" class="attach-item">
                    <a
                      class="attach-item__name"
                      :href="file.url"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ file.name }}
                    </a>
                    <span class="attach-item__size">{{ formatSize(file.size) }}</span>
                    <button
                      v-permission="'meeting:modify'"
                      type="button"
                      class="attach-item__remove"
                      @click="removeAttachment(file.id)"
                    >
                      删除
                    </button>
                  </li>
                </ul>
                <p v-else class="attach-box__hint">暂无材料，支持 txt / md / 文档等</p>

                <div class="ai-row">
                  <button
                    v-permission="'ai:meeting:summary'"
                    type="button"
                    class="wb-btn wb-btn--primary"
                    :disabled="summarizing || !currentAttachments.length"
                    @click="runAiSummary"
                  >
                    <ThunderboltOutlined />
                    {{ summarizing ? '整理中…' : 'AI 整理' }}
                  </button>
                  <span class="ai-row__hint">根据已上传材料自动生成会议概要</span>
                </div>
              </template>
            </div>
          </a-form-item>

          <a-form-item
            v-if="currentSummary"
            label="AI 会议概要"
            class="dialog-item dialog-item--full"
          >
            <div class="summary-box">{{ currentSummary }}</div>
          </a-form-item>
        </div>
      </a-form>
    </DataOperationView>
  </div>
</template>

<style scoped>
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
  min-height: 280px;
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
  margin: 0 0 8px;
  max-width: 36ch;
  font-size: 0.88rem;
  line-height: 1.55;
  color: var(--color-text-secondary);
}

.meeting-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meeting-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.meeting-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.meeting-card__main {
  min-width: 0;
  flex: 1;
}

.meeting-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.meeting-card__time {
  font-size: 0.8rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-accent-deep);
}

.meeting-card__files {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-dim);
}

.meeting-card__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.meeting-card__sub {
  margin: 6px 0 0;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.meeting-card__summary {
  margin: 10px 0 0;
  max-height: 4.8em;
  overflow: hidden;
  font-size: 0.84rem;
  line-height: 1.55;
  color: var(--color-text-body);
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.meeting-card__actions {
  display: flex;
  flex-shrink: 0;
  gap: 6px;
}

.is-danger:hover {
  color: var(--color-red);
  border-color: rgba(224, 85, 69, 0.35);
}

.attach-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attach-box__hint {
  margin: 0;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.attach-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(238, 248, 252, 0.7);
  border: 1px solid var(--color-border-light);
}

.attach-item__name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-accent-deep);
}

.attach-item__size {
  font-size: 0.75rem;
  color: var(--color-text-dim);
  font-variant-numeric: tabular-nums;
}

.attach-item__remove {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.78rem;
  font-weight: 650;
  cursor: pointer;
}

.attach-item__remove:hover {
  color: var(--color-red);
}

.ai-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.ai-row__hint {
  font-size: 0.78rem;
  color: var(--color-text-dim);
}

.summary-box {
  max-height: 280px;
  overflow: auto;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(238, 248, 252, 0.65);
  border: 1px solid var(--color-border-light);
  white-space: pre-wrap;
  font-size: 0.88rem;
  line-height: 1.6;
  color: var(--color-text-body);
}

@media (max-width: 768px) {
  .meeting-card {
    flex-direction: column;
  }

  .meeting-card__actions {
    width: 100%;
  }

  .meeting-card__actions .wb-btn {
    flex: 1;
  }
}
</style>
