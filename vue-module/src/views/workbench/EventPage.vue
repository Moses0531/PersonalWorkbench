<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
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

const WEEKDAY_HEADERS = ['一', '二', '三', '四', '五', '六', '日']
const MONTH_EVENT_LIMIT = 3

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const events = ref([])
const tasks = ref([])
const searchQuery = ref('')
const viewMode = ref('month')
const cursorDate = ref(dayjs())
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))

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

const eventsByDate = computed(() => {
  const map = new Map()
  for (const ev of filteredEvents.value) {
    const key = dayjs(ev.startTime).format('YYYY-MM-DD')
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(ev)
  }
  return map
})

const periodLabel = computed(() => {
  const d = cursorDate.value
  if (viewMode.value === 'week') {
    const start = d.startOf('week')
    const end = d.endOf('week')
    if (start.month() === end.month()) {
      return `${start.format('YYYY年M月D日')} – ${end.format('D日')}`
    }
    return `${start.format('YYYY年M月D日')} – ${end.format('M月D日')}`
  }
  return d.format('YYYY年M月')
})

const calendarDays = computed(() => {
  const cursor = cursorDate.value
  let start
  let end
  if (viewMode.value === 'week') {
    start = cursor.startOf('week')
    end = cursor.endOf('week')
  } else {
    start = cursor.startOf('month').startOf('week')
    end = cursor.startOf('month').endOf('month').endOf('week')
  }

  const todayKey = dayjs().format('YYYY-MM-DD')
  const monthKey = cursor.format('YYYY-MM')
  const days = []
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end, 'day')) {
    const key = cur.format('YYYY-MM-DD')
    const dayEvents = eventsByDate.value.get(key) || []
    days.push({
      key,
      date: cur,
      dayNum: cur.date(),
      weekdayLabel: WEEKDAY_HEADERS[(cur.day() + 6) % 7],
      isToday: key === todayKey,
      isSelected: key === selectedDate.value,
      isOutside: viewMode.value === 'month' && cur.format('YYYY-MM') !== monthKey,
      isWeekend: cur.day() === 0 || cur.day() === 6,
      events: dayEvents,
      overflow: Math.max(0, dayEvents.length - MONTH_EVENT_LIMIT),
    })
    cur = cur.add(1, 'day')
  }
  return days
})

const selectedDayEvents = computed(() => eventsByDate.value.get(selectedDate.value) || [])

const selectedDayLabel = computed(() => formatDayLabel(selectedDate.value))

const stats = computed(() => {
  const all = events.value
  const todayKey = dayjs().format('YYYY-MM-DD')
  const today = all.filter((e) => dayjs(e.startTime).format('YYYY-MM-DD') === todayKey).length
  const upcoming = all.filter((e) => !dayjs(e.startTime).isBefore(dayjs(), 'day')).length
  const repeating = all.filter((e) => String(e.repeatType) === '1').length
  return { total: all.length, today, upcoming, repeating }
})

watch(viewMode, () => {
  const selected = dayjs(selectedDate.value)
  if (selected.isValid()) cursorDate.value = selected
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

function shiftPeriod(delta) {
  const unit = viewMode.value === 'week' ? 'week' : 'month'
  cursorDate.value = cursorDate.value.add(delta, unit)
}

function goToday() {
  const today = dayjs()
  cursorDate.value = today
  selectedDate.value = today.format('YYYY-MM-DD')
}

function selectDay(day) {
  selectedDate.value = day.key
  if (viewMode.value === 'month' && day.isOutside) {
    cursorDate.value = day.date
  }
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
  resetForm(dateStr || selectedDate.value)
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

function onEventChipClick(ev, e) {
  e.stopPropagation()
  selectedDate.value = dayjs(ev.startTime).format('YYYY-MM-DD')
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
      selectedDate.value = dayjs(payload.startTime).format('YYYY-MM-DD')
      cursorDate.value = dayjs(payload.startTime)
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
          <p class="wb-header__desc">按周或按月查看个人日程，支持全天与每周重复。</p>
        </div>
        <div class="wb-header__actions">
          <button type="button" class="wb-btn wb-btn--ghost" :disabled="loading" @click="refreshAll">刷新</button>
          <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate()">
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
        <div class="range-tabs" role="tablist" aria-label="日历视图">
          <button
            type="button"
            class="range-tab"
            :class="{ 'is-active': viewMode === 'week' }"
            role="tab"
            :aria-selected="viewMode === 'week'"
            @click="viewMode = 'week'"
          >
            周视图
          </button>
          <button
            type="button"
            class="range-tab"
            :class="{ 'is-active': viewMode === 'month' }"
            role="tab"
            :aria-selected="viewMode === 'month'"
            @click="viewMode = 'month'"
          >
            月视图
          </button>
        </div>
      </div>

      <a-spin :spinning="loading">
        <div class="cal-layout">
          <section class="cal-panel" aria-label="日程日历">
            <div class="cal-nav">
              <div class="cal-nav__period">
                <h2 class="cal-nav__title">{{ periodLabel }}</h2>
                <span class="cal-nav__hint">{{ viewMode === 'week' ? '本周安排' : '本月安排' }}</span>
              </div>
              <div class="cal-nav__actions">
                <button type="button" class="wb-btn wb-btn--ghost cal-nav__btn" aria-label="上一周期" @click="shiftPeriod(-1)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="15 18 9 12 15 6" />
                  </svg>
                </button>
                <button type="button" class="wb-btn wb-btn--ghost cal-nav__today" @click="goToday">今天</button>
                <button type="button" class="wb-btn wb-btn--ghost cal-nav__btn" aria-label="下一周期" @click="shiftPeriod(1)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="cal-weekdays" aria-hidden="true">
              <span v-for="label in WEEKDAY_HEADERS" :key="label" class="cal-weekday">{{ label }}</span>
            </div>

            <div
              class="cal-grid"
              :class="viewMode === 'week' ? 'cal-grid--week' : 'cal-grid--month'"
              role="grid"
              :aria-label="periodLabel"
            >
              <div
                v-for="day in calendarDays"
                :key="day.key"
                class="cal-cell"
                :class="{
                  'is-today': day.isToday,
                  'is-selected': day.isSelected,
                  'is-outside': day.isOutside,
                  'is-weekend': day.isWeekend,
                  'has-events': day.events.length > 0,
                }"
                role="gridcell"
                tabindex="0"
                :aria-selected="day.isSelected"
                :aria-label="`${day.key}，${day.events.length} 项日程`"
                @click="selectDay(day)"
                @dblclick="openCreate(day.key)"
                @keydown.enter.prevent="selectDay(day)"
                @keydown.space.prevent="selectDay(day)"
              >
                <div class="cal-cell__head">
                  <span class="cal-cell__day">{{ day.dayNum }}</span>
                  <span v-if="viewMode === 'week'" class="cal-cell__wd">周{{ day.weekdayLabel }}</span>
                  <span v-if="day.events.length" class="cal-cell__count">{{ day.events.length }}</span>
                </div>

                <div class="cal-cell__events">
                  <button
                    v-for="ev in day.events.slice(0, viewMode === 'week' ? 6 : MONTH_EVENT_LIMIT)"
                    :key="ev.eventId"
                    type="button"
                    class="cal-chip"
                    :class="{ 'is-allday': Number(ev.isAllDay) === 1 }"
                    :title="ev.title"
                    @click="onEventChipClick(ev, $event)"
                  >
                    <em class="cal-chip__time">{{ formatChipTime(ev) }}</em>
                    <span class="cal-chip__title">{{ ev.title }}</span>
                  </button>
                  <span v-if="viewMode === 'month' && day.overflow > 0" class="cal-more">
                    +{{ day.overflow }} 更多
                  </span>
                </div>
              </div>
            </div>
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
                @click="openCreate(selectedDate)"
              >
                + 添加
              </button>
            </header>

            <div v-if="!selectedDayEvents.length" class="day-panel__empty">
              <p class="day-panel__empty-title">这天还没有安排</p>
              <p class="day-panel__empty-desc">双击日历格子，或点下方按钮快速新建。</p>
              <button v-permission="'event:add'" type="button" class="wb-btn wb-btn--primary" @click="openCreate(selectedDate)">
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
  transition: background 0.2s ease, color 0.2s ease, transform 0.15s ease;
}

.range-tab:hover {
  color: var(--color-text-primary);
}

.range-tab:active {
  transform: scale(0.98);
}

.range-tab.is-active {
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
}

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
  padding: var(--space-4);
  overflow: hidden;
}

.cal-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--space-4);
}

.cal-nav__title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.cal-nav__hint {
  display: block;
  margin-top: 2px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-dim);
}

.cal-nav__actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.cal-nav__btn {
  width: 36px;
  padding: 0;
}

.cal-nav__today {
  min-width: 56px;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
  margin-bottom: 6px;
  padding: 0 2px;
}

.cal-weekday {
  text-align: center;
  font-size: 0.72rem;
  font-weight: 650;
  letter-spacing: 0.04em;
  color: var(--color-text-dim);
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}

.cal-grid--month .cal-cell {
  min-height: 104px;
}

.cal-grid--week .cal-cell {
  min-height: 220px;
}

.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: rgba(238, 248, 252, 0.55);
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;
}

.cal-cell:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: var(--color-border);
}

.cal-cell:active {
  transform: scale(0.99);
}

.cal-cell:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--color-accent-soft);
  border-color: var(--color-accent);
}

.cal-cell.is-weekend {
  background: rgba(8, 145, 178, 0.04);
}

.cal-cell.is-outside {
  opacity: 0.45;
}

.cal-cell.is-today {
  background: linear-gradient(160deg, rgba(8, 145, 178, 0.12) 0%, rgba(255, 255, 255, 0.92) 70%);
  border-color: rgba(8, 145, 178, 0.22);
}

.cal-cell.is-selected {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 2px var(--color-accent-soft), var(--shadow-sm);
  background: #fff;
}

.cal-cell__head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 22px;
}

.cal-cell__day {
  font-size: 0.875rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
  line-height: 1;
}

.cal-cell.is-today .cal-cell__day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-deep) 100%);
}

.cal-cell__wd {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-dim);
}

.cal-cell__count {
  margin-left: auto;
  font-size: 0.68rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-accent-deep);
  background: var(--color-accent-soft);
  border-radius: 6px;
  padding: 1px 6px;
}

.cal-cell__events {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 0;
  flex: 1;
}

.cal-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  padding: 3px 6px;
  border-radius: 6px;
  border: none;
  background: rgba(8, 145, 178, 0.12);
  color: var(--color-accent-deep);
  font-size: 0.7rem;
  font-weight: 650;
  line-height: 1.2;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.15s ease;
}

.cal-chip:hover {
  background: rgba(8, 145, 178, 0.2);
}

.cal-chip:active {
  transform: scale(0.98);
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

  .cal-grid--week .cal-cell {
    min-height: 160px;
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

  .cal-grid--month .cal-cell {
    min-height: 72px;
    padding: 6px;
  }

  .cal-chip__time {
    display: none;
  }

  .cal-grid--week {
    grid-template-columns: 1fr;
  }

  .cal-weekdays {
    display: none;
  }

  .cal-grid--week .cal-cell {
    min-height: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cal-cell,
  .cal-chip,
  .range-tab,
  .day-item {
    transition: none;
  }
}
</style>
