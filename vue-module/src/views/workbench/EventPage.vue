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
import { PlusOutlined } from '@ant-design/icons-vue'

const WEEKDAY_OPTIONS = [
  { value: '1', label: '一' },
  { value: '2', label: '二' },
  { value: '3', label: '三' },
  { value: '4', label: '四' },
  { value: '5', label: '五' },
  { value: '6', label: '六' },
  { value: '7', label: '日' },
]

const CELL_EVENT_LIMIT = 3

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const events = ref([])
const tasks = ref([])
const searchQuery = ref('')
const calendarValue = ref(dayjs())

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
  return events.value
    .filter((ev) => {
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

const selectedDateKey = computed(() => calendarValue.value.format('YYYY-MM-DD'))
const selectedDayEvents = computed(() => eventsOf(calendarValue.value))
const selectedDayLabel = computed(() => formatDayLabel(selectedDateKey.value))

const stats = computed(() => {
  const all = events.value
  const today = dayjs().startOf('day')
  const todayCount = all.filter((e) => eventOccursOn(e, today)).length
  const upcoming = all.filter((e) => {
    if (String(e.repeatType) === '1') return true
    return !dayjs(e.startTime).isBefore(today, 'day')
  }).length
  const repeating = all.filter((e) => String(e.repeatType) === '1').length
  return { total: all.length, today: todayCount, upcoming, repeating }
})

/** 1=周一 … 7=周日，与后端 repeat_weekdays 一致 */
function weekdayKey(day) {
  return day.day() === 0 ? '7' : String(day.day())
}

/** 判断日程是否在指定日期出现（含每周重复展开） */
function eventOccursOn(ev, day) {
  const start = dayjs(ev.startTime)
  if (!start.isValid()) return false
  const dayKey = day.format('YYYY-MM-DD')
  if (start.format('YYYY-MM-DD') === dayKey) return true
  if (String(ev.repeatType) !== '1') return false
  if (start.startOf('day').isAfter(day, 'day')) return false
  const days = String(ev.repeatWeekdays || '')
    .split(',')
    .map((d) => d.trim())
    .filter(Boolean)
  const weekday = weekdayKey(day)
  if (!days.length) return weekday === weekdayKey(start)
  return days.includes(weekday)
}

function eventsOf(date) {
  const day = dayjs(date).startOf('day')
  return filteredEvents.value.filter((ev) => eventOccursOn(ev, day))
}

function eventsInMonth(date) {
  const monthStart = date.startOf('month')
  const monthEnd = date.endOf('month')
  return filteredEvents.value.filter((ev) => {
    const start = dayjs(ev.startTime)
    if (!start.isValid()) return false
    if (String(ev.repeatType) !== '1') {
      return start.format('YYYY-MM') === date.format('YYYY-MM')
    }
    if (start.isAfter(monthEnd, 'day')) return false
    let d = monthStart.isBefore(start, 'day') ? start.startOf('day') : monthStart
    while (!d.isAfter(monthEnd, 'day')) {
      if (eventOccursOn(ev, d)) return true
      d = d.add(1, 'day')
    }
    return false
  })
}

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

function formatChipTime(ev) {
  if (Number(ev.isAllDay) === 1) return '全天'
  const s = dayjs(ev.startTime)
  return s.isValid() ? s.format('HH:mm') : ''
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

function onCalendarSelect(value) {
  calendarValue.value = value
}

function resetForm(dateStr) {
  const base = dateStr && dayjs(dateStr).isValid() ? dayjs(dateStr) : dayjs()
  const start = base.hour(9).minute(0).second(0)
  Object.assign(form, {
    eventId: null,
    taskId: null,
    title: '',
    location: '',
    range: [start, start.add(1, 'hour')],
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

function openCreate(dateStr) {
  isEdit.value = false
  resetForm(dateStr || selectedDateKey.value)
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

function onEventChipClick(ev, e, date) {
  e.stopPropagation()
  if (date) calendarValue.value = dayjs(date)
  openEdit(ev)
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
    if (payload.startTime) {
      calendarValue.value = dayjs(payload.startTime)
    }
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
          <p class="wb-header__desc">按月查看个人日程，支持全天与每周重复。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="refreshAll">刷新</button>
          <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate()">
            <PlusOutlined />
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
      </div>

      <a-spin :spinning="loading">
        <div class="cal-layout">
          <section class="cal-panel" aria-label="日程日历">
            <a-calendar
              v-model:value="calendarValue"
              @select="onCalendarSelect"
            >
              <template #dateCellRender="{ current }">
                <ul class="cal-events" @dblclick.stop="openCreate(current.format('YYYY-MM-DD'))">
                  <li
                    v-for="ev in eventsOf(current).slice(0, CELL_EVENT_LIMIT)"
                    :key="ev.eventId"
                    class="cal-chip"
                    :class="{ 'is-allday': Number(ev.isAllDay) === 1 }"
                    :title="ev.title"
                    @click.stop="onEventChipClick(ev, $event, current)"
                  >
                    <em class="cal-chip__time">{{ formatChipTime(ev) }}</em>
                    <span class="cal-chip__title">{{ ev.title }}</span>
                  </li>
                  <li
                    v-if="eventsOf(current).length > CELL_EVENT_LIMIT"
                    class="cal-more"
                  >
                    +{{ eventsOf(current).length - CELL_EVENT_LIMIT }} 更多
                  </li>
                </ul>
              </template>
              <template #monthCellRender="{ current }">
                <div v-if="eventsInMonth(current).length" class="cal-month-count">
                  {{ eventsInMonth(current).length }} 项
                </div>
              </template>
            </a-calendar>
          </section>

          <aside class="day-panel" aria-label="当日日程">
            <header class="day-panel__head">
              <div>
                <h2 class="day-panel__title">{{ selectedDayLabel }}</h2>
                <p class="day-panel__meta">{{ selectedDayEvents.length }} 项日程</p>
              </div>
              <button
                v-permission="'event:add'"
                type="button"
                class="wb-btn wb-btn--ghost day-panel__add"
                @click="openCreate(selectedDateKey)"
              >
                + 添加
              </button>
            </header>

            <div v-if="!selectedDayEvents.length" class="day-panel__empty">
              <p class="day-panel__empty-title">这天还没有安排</p>
              <p class="day-panel__empty-desc">双击日历格子，或点下方按钮快速新建。</p>
              <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate(selectedDateKey)">
                新建日程
              </button>
            </div>

            <ul v-else class="day-list">
              <li v-for="ev in selectedDayEvents" :key="ev.eventId" class="day-item">
                <div class="day-item__time">
                  <span class="day-item__clock">{{ formatTimeRange(ev) }}</span>
                  <span v-if="repeatLabel(ev)" class="wb-chip wb-chip--active">{{ repeatLabel(ev) }}</span>
                </div>
                <div class="day-item__body">
                  <h3 class="day-item__title">{{ ev.title }}</h3>
                  <p v-if="ev.location" class="day-item__loc">{{ ev.location }}</p>
                  <p v-if="taskTitleOf(ev.taskId)" class="day-item__task">关联任务：{{ taskTitleOf(ev.taskId) }}</p>
                  <p v-if="ev.remark" class="day-item__remark">{{ ev.remark }}</p>
                </div>
                <div class="day-item__actions">
                  <button
                    v-permission="'event:modify'"
                    type="button"
                    class="wb-btn wb-btn--ghost day-item__btn"
                    @click="openEdit(ev)"
                  >
                    编辑
                  </button>
                  <a-popconfirm
                    v-permission="'event:remove'"
                    title="确认删除该日程？"
                    @confirm="removeEvent(ev.eventId)"
                  >
                    <button type="button" class="wb-btn wb-btn--ghost day-item__btn is-danger">删除</button>
                  </a-popconfirm>
                </div>
              </li>
            </ul>
          </aside>
        </div>
      </a-spin>
    </div>

    <DataOperationView
      v-model="dialogVisible"
      :title="isEdit ? '编辑日程' : '新建日程'"
      :columns="2"
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
.cal-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: var(--space-4);
  align-items: start;
}

.cal-panel,
.day-panel {
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-xs);
}

.cal-panel {
  padding: var(--space-3);
  overflow: hidden;
}

.cal-panel :deep(.ant-picker-calendar) {
  background: transparent;
}

.cal-panel :deep(.ant-picker-calendar-header) {
  padding: 8px 4px 12px;
}

.cal-panel :deep(.ant-picker-panel) {
  background: transparent;
  border-top: none;
}

.cal-panel :deep(.ant-picker-calendar-date-content) {
  height: 86px;
  overflow-y: auto;
}

.cal-events {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.cal-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(8, 145, 178, 0.12);
  color: var(--color-accent-deep);
  font-size: 0.7rem;
  font-weight: 650;
  line-height: 1.2;
  cursor: pointer;
  transition: background 0.15s ease;
}

.cal-chip:hover {
  background: rgba(8, 145, 178, 0.2);
}

.cal-chip.is-allday {
  background: rgba(74, 186, 106, 0.14);
  color: #2f8a4a;
}

.cal-chip__time {
  flex-shrink: 0;
  font-style: normal;
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
}

.cal-chip__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cal-more {
  font-size: 0.68rem;
  font-weight: 650;
  color: var(--color-text-secondary);
  padding: 1px 4px;
}

.cal-month-count {
  font-size: 0.85rem;
  font-weight: 650;
  color: var(--color-accent-deep);
}

.day-panel {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  min-height: 360px;
  max-height: min(720px, calc(100vh - 120px));
  padding: var(--space-4);
}

.day-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--space-4);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.day-panel__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.day-panel__meta {
  margin: 4px 0 0;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-dim);
  font-variant-numeric: tabular-nums;
}

.day-panel__add {
  height: 32px;
  flex-shrink: 0;
}

.day-panel__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  padding: var(--space-4);
}

.day-panel__empty-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 650;
  color: var(--color-text-primary);
}

.day-panel__empty-desc {
  margin: 0 0 6px;
  max-width: 26ch;
  font-size: 0.82rem;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.day-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}

.day-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(238, 248, 252, 0.65);
  border: 1px solid var(--color-border-light);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.day-item:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-xs);
}

.day-item__time {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.day-item__clock {
  font-size: 0.82rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--color-accent-deep);
}

.day-item__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.day-item__loc,
.day-item__task,
.day-item__remark {
  margin: 6px 0 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--color-text-secondary);
}

.day-item__task {
  color: var(--color-accent-deep);
  font-weight: 600;
}

.day-item__actions {
  display: flex;
  gap: 6px;
}

.day-item__btn {
  height: 30px;
  min-width: 56px;
}

.day-item__btn.is-danger:hover {
  color: var(--color-red);
  border-color: rgba(224, 85, 69, 0.35);
}

@media (max-width: 1100px) {
  .cal-layout {
    grid-template-columns: 1fr;
  }

  .day-panel {
    position: static;
    max-height: none;
    min-height: 0;
  }
}

@media (max-width: 768px) {
  .wb-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .wb-search {
    max-width: none;
  }

  .cal-panel :deep(.ant-picker-calendar-date-content) {
    height: 56px;
  }

  .cal-chip__time {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cal-chip,
  .day-item {
    transition: none;
  }
}
</style>
