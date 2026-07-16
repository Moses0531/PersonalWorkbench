<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  addEventApi,
  deleteEventsApi,
  pageEventsApi,
  updateEventApi,
} from '@/apis/workbench/EventApi'
import { pageTasksApi } from '@/apis/workbench/TaskApi'
import DataOperationView from '@/components/ListView/DataOperationView.vue'

const WEEKDAY_OPTIONS = [
  { value: '1', label: '一' },
  { value: '2', label: '二' },
  { value: '3', label: '三' },
  { value: '4', label: '四' },
  { value: '5', label: '五' },
  { value: '6', label: '六' },
  { value: '7', label: '日' },
]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const events = ref([])
const tasks = ref([])
const searchQuery = ref('')
const rangeMode = ref('upcoming')

const form = reactive({
  eventId: null,
  taskId: null,
  title: '',
  location: '',
  range: [],
  isAllDay: 0,
  repeatType: '0',
  repeatWeekdays: [],
  displayOrder: 0,
  remark: '',
})

const taskMap = computed(() => {
  const map = new Map()
  for (const t of tasks.value) map.set(Number(t.taskId), t.title)
  return map
})

const filteredEvents = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const now = dayjs().startOf('day')
  return events.value
    .filter((ev) => {
      const start = dayjs(ev.startTime)
      if (rangeMode.value === 'upcoming' && start.isBefore(now, 'day')) return false
      if (rangeMode.value === 'past' && !start.isBefore(now, 'day')) return false
      if (!q) return true
      return (
        (ev.title || '').toLowerCase().includes(q) ||
        (ev.location || '').toLowerCase().includes(q) ||
        (ev.remark || '').toLowerCase().includes(q)
      )
    })
    .slice()
    .sort((a, b) => dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf())
})

const agendaGroups = computed(() => {
  const groups = new Map()
  for (const ev of filteredEvents.value) {
    const key = dayjs(ev.startTime).format('YYYY-MM-DD')
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(ev)
  }
  return [...groups.entries()].map(([date, items]) => ({
    date,
    label: formatDayLabel(date),
    items,
  }))
})

const stats = computed(() => {
  const all = events.value
  const todayKey = dayjs().format('YYYY-MM-DD')
  const today = all.filter((e) => dayjs(e.startTime).format('YYYY-MM-DD') === todayKey).length
  const upcoming = all.filter((e) => !dayjs(e.startTime).isBefore(dayjs(), 'day')).length
  const repeating = all.filter((e) => String(e.repeatType) === '1').length
  return { total: all.length, today, upcoming, repeating }
})

function formatDayLabel(dateStr) {
  const d = dayjs(dateStr)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const today = dayjs().startOf('day')
  const diff = d.startOf('day').diff(today, 'day')
  let relative = d.format('M月D日')
  if (diff === 0) relative = '今天'
  else if (diff === 1) relative = '明天'
  else if (diff === -1) relative = '昨天'
  return `${relative} · 周${weekdays[d.day()]}`
}

function formatTimeRange(ev) {
  if (Number(ev.isAllDay) === 1) return '全天'
  const s = dayjs(ev.startTime)
  const e = dayjs(ev.endTime)
  if (!s.isValid()) return '-'
  const start = s.format('HH:mm')
  const end = e.isValid() ? e.format('HH:mm') : ''
  return end ? `${start} - ${end}` : start
}

function repeatLabel(ev) {
  if (String(ev.repeatType) !== '1') return ''
  const days = String(ev.repeatWeekdays || '')
    .split(',')
    .map((d) => d.trim())
    .filter(Boolean)
  if (!days.length) return '每周'
  const labels = days
    .map((d) => WEEKDAY_OPTIONS.find((o) => o.value === d)?.label || d)
    .join('')
  return `每周${labels}`
}

function taskTitleOf(taskId) {
  if (taskId == null || taskId === '') return ''
  return taskMap.value.get(Number(taskId)) || ''
}

function resetForm() {
  Object.assign(form, {
    eventId: null,
    taskId: null,
    title: '',
    location: '',
    range: [dayjs().hour(9).minute(0).second(0), dayjs().hour(10).minute(0).second(0)],
    isAllDay: 0,
    repeatType: '0',
    repeatWeekdays: [],
    displayOrder: 0,
    remark: '',
  })
}

async function loadTasks() {
  try {
    const result = await pageTasksApi(1, null)
    tasks.value = (result?.data?.records || []).filter((t) => String(t.status) !== '3')
  } catch {
    tasks.value = []
  }
}

async function loadEvents() {
  loading.value = true
  try {
    const result = await pageEventsApi(1, null)
    events.value = result?.data?.records || []
  } catch (error) {
    message.error(error.message || '获取日程列表失败')
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadEvents(), loadTasks()])
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  const start = row.startTime ? dayjs(row.startTime) : dayjs()
  const end = row.endTime ? dayjs(row.endTime) : start.add(1, 'hour')
  Object.assign(form, {
    eventId: row.eventId,
    taskId: row.taskId != null ? Number(row.taskId) : null,
    title: row.title || '',
    location: row.location || '',
    range: [start, end],
    isAllDay: Number(row.isAllDay) === 1 ? 1 : 0,
    repeatType: row.repeatType != null ? String(row.repeatType) : '0',
    repeatWeekdays: String(row.repeatWeekdays || '')
      .split(',')
      .map((d) => d.trim())
      .filter(Boolean),
    displayOrder: row.displayOrder ?? 0,
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

function toPayload() {
  const [start, end] = form.range || []
  const startTime = start ? dayjs(start) : null
  const endTime = end ? dayjs(end) : null
  return {
    eventId: form.eventId,
    taskId: form.taskId != null ? Number(form.taskId) : null,
    title: form.title.trim(),
    location: form.location?.trim() || '',
    startTime: startTime?.isValid() ? startTime.format('YYYY-MM-DD HH:mm:ss') : null,
    endTime: endTime?.isValid() ? endTime.format('YYYY-MM-DD HH:mm:ss') : null,
    isAllDay: Number(form.isAllDay) === 1 ? 1 : 0,
    repeatType: String(form.repeatType ?? '0'),
    repeatWeekdays:
      String(form.repeatType) === '1' ? (form.repeatWeekdays || []).join(',') : '',
    displayOrder: Number(form.displayOrder) || 0,
    remark: form.remark?.trim() || '',
  }
}

async function submitForm() {
  if (!form.title?.trim()) {
    message.warning('请填写日程标题')
    return
  }
  if (!form.range?.[0] || !form.range?.[1]) {
    message.warning('请选择开始与结束时间')
    return
  }
  if (dayjs(form.range[1]).isBefore(dayjs(form.range[0]))) {
    message.warning('结束时间不能早于开始时间')
    return
  }
  submitting.value = true
  try {
    const payload = toPayload()
    if (isEdit.value) await updateEventApi(payload)
    else await addEventApi(payload)
    message.success('操作成功')
    dialogVisible.value = false
    await loadEvents()
  } catch (error) {
    message.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function removeEvent(id) {
  try {
    await deleteEventsApi(id)
    message.success('删除成功')
    await loadEvents()
  } catch (error) {
    message.error(error.message || '删除失败')
  }
}

onMounted(refreshAll)
</script>

<template>
  <div class="wb-page event-page">
    <div class="wb-page__blob wb-page__blob--1" aria-hidden="true" />
    <div class="wb-page__blob wb-page__blob--2" aria-hidden="true" />

    <div class="wb-page__inner">
      <header class="wb-header">
        <div class="wb-header__text">
          <h1 class="wb-header__title">日程管理</h1>
          <p class="wb-header__desc">按日浏览个人日程，支持全天与每周重复。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="refreshAll">刷新</button>
          <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新建日程
          </button>
        </div>
      </header>

      <div class="wb-stats">
        <div class="wb-stat wb-stat--accent">
          <span class="wb-stat__label">全部日程</span>
          <span class="wb-stat__value">{{ stats.total }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">今日</span>
          <span class="wb-stat__value">{{ stats.today }}</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">未开始</span>
          <span class="wb-stat__value">{{ stats.upcoming }}</span>
          <span class="wb-stat__hint">含今天及之后</span>
        </div>
        <div class="wb-stat">
          <span class="wb-stat__label">每周重复</span>
          <span class="wb-stat__value">{{ stats.repeating }}</span>
        </div>
      </div>

      <div class="wb-toolbar">
        <input
          v-model="searchQuery"
          type="search"
          class="wb-search"
          placeholder="搜索标题、地点..."
          aria-label="搜索日程"
        />
        <div class="range-tabs" role="tablist" aria-label="时间范围">
          <button
            type="button"
            class="range-tab"
            :class="{ 'is-active': rangeMode === 'upcoming' }"
            role="tab"
            :aria-selected="rangeMode === 'upcoming'"
            @click="rangeMode = 'upcoming'"
          >
            即将到来
          </button>
          <button
            type="button"
            class="range-tab"
            :class="{ 'is-active': rangeMode === 'all' }"
            role="tab"
            :aria-selected="rangeMode === 'all'"
            @click="rangeMode = 'all'"
          >
            全部
          </button>
          <button
            type="button"
            class="range-tab"
            :class="{ 'is-active': rangeMode === 'past' }"
            role="tab"
            :aria-selected="rangeMode === 'past'"
            @click="rangeMode = 'past'"
          >
            已过去
          </button>
        </div>
      </div>

      <a-spin :spinning="loading">
        <div v-if="!loading && !agendaGroups.length" class="wb-empty">
          <h2 class="wb-empty__title">这段时间没有日程</h2>
          <p class="wb-empty__desc">安排一次会议、提醒或专注时段，它会出现在议程里。</p>
          <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate">
            新建日程
          </button>
        </div>

        <div v-else class="agenda">
          <section v-for="group in agendaGroups" :key="group.date" class="agenda-day">
            <header class="agenda-day__head">
              <h2 class="agenda-day__title">{{ group.label }}</h2>
              <span class="agenda-day__count">{{ group.items.length }} 项</span>
            </header>

            <ul class="agenda-list">
              <li v-for="ev in group.items" :key="ev.eventId" class="agenda-item">
                <div class="agenda-item__time">
                  <span class="agenda-item__clock">{{ formatTimeRange(ev) }}</span>
                  <span v-if="repeatLabel(ev)" class="wb-chip wb-chip--active">{{ repeatLabel(ev) }}</span>
                </div>

                <div class="agenda-item__body">
                  <h3 class="agenda-item__title">{{ ev.title }}</h3>
                  <p v-if="ev.location" class="agenda-item__loc">{{ ev.location }}</p>
                  <p v-if="taskTitleOf(ev.taskId)" class="agenda-item__task">
                    关联任务：{{ taskTitleOf(ev.taskId) }}
                  </p>
                  <p v-if="ev.remark" class="agenda-item__remark">{{ ev.remark }}</p>
                </div>

                <div class="agenda-item__actions">
                  <button
                    v-permission="'event:modify'"
                    type="button"
                    class="wb-btn wb-btn--ghost agenda-item__btn"
                    @click="openEdit(ev)"
                  >
                    编辑
                  </button>
                  <a-popconfirm
                    v-permission="'event:remove'"
                    title="确认删除该日程？"
                    @confirm="removeEvent(ev.eventId)"
                  >
                    <button type="button" class="wb-btn wb-btn--ghost agenda-item__btn is-danger">删除</button>
                  </a-popconfirm>
                </div>
              </li>
            </ul>
          </section>
        </div>
      </a-spin>
    </div>

    <DataOperationView
      v-model="dialogVisible"
      :title="isEdit ? '编辑日程' : '新建日程'"
      width="560px"
      :loading="submitting"
      :confirm-text="isEdit ? '保存修改' : '确认创建'"
      @confirm="submitForm"
    >
      <a-form layout="vertical" :model="form" class="dialog-form">
        <div class="dialog-grid">
          <a-form-item label="标题" class="dialog-item dialog-item--full" required>
            <a-input v-model:value.trim="form.title" placeholder="例如：周会 / 专注写作" :maxlength="120" />
          </a-form-item>

          <a-form-item label="时间" class="dialog-item dialog-item--full" required>
            <a-range-picker
              v-model:value="form.range"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item label="地点" class="dialog-item">
            <a-input v-model:value.trim="form.location" placeholder="选填" />
          </a-form-item>

          <a-form-item label="全天" class="dialog-item">
            <a-select v-model:value="form.isAllDay" style="width: 100%">
              <a-select-option :value="0">否</a-select-option>
              <a-select-option :value="1">是</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="重复" class="dialog-item">
            <a-select v-model:value="form.repeatType" style="width: 100%">
              <a-select-option value="0">不重复</a-select-option>
              <a-select-option value="1">每周</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="form.repeatType === '1'"
            label="重复星期"
            class="dialog-item"
          >
            <a-select
              v-model:value="form.repeatWeekdays"
              mode="multiple"
              placeholder="选择星期"
              style="width: 100%"
              :options="WEEKDAY_OPTIONS"
            />
          </a-form-item>

          <a-form-item label="关联任务" class="dialog-item dialog-item--full">
            <a-select v-model:value="form.taskId" allow-clear placeholder="可选" style="width: 100%">
              <a-select-option v-for="t in tasks" :key="t.taskId" :value="Number(t.taskId)">
                {{ t.title }}
              </a-select-option>
            </a-select>
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
.range-tabs {
  display: inline-flex;
  padding: 3px;
  border-radius: var(--radius-control);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--color-border-light);
}

.range-tab {
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.range-tab.is-active {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

.agenda {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.agenda-day__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

.agenda-day__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.agenda-day__count {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-dim);
  font-variant-numeric: tabular-nums;
}

.agenda-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agenda-item {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
  padding: 14px 16px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.agenda-item:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.agenda-item__time {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.agenda-item__clock {
  font-size: 0.875rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--color-accent-deep);
}

.agenda-item__title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.agenda-item__loc,
.agenda-item__task,
.agenda-item__remark {
  margin: 6px 0 0;
  font-size: 0.82rem;
  line-height: 1.45;
  color: var(--color-text-secondary);
}

.agenda-item__task {
  color: var(--color-accent-deep);
  font-weight: 600;
}

.agenda-item__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agenda-item__btn {
  height: 32px;
  min-width: 64px;
}

.agenda-item__btn.is-danger:hover {
  color: var(--color-red);
  border-color: rgba(224, 85, 69, 0.35);
}

@media (max-width: 768px) {
  .agenda-item {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .agenda-item__actions {
    flex-direction: row;
  }

  .wb-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .wb-search {
    max-width: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .agenda-item,
  .range-tab {
    transition: none;
  }
}
</style>
