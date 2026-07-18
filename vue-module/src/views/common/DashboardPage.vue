<script setup>
/**
 * 工作台首页
 * 构图对齐登录页：Outfit 品牌 + 衬线问候 + 玻璃面板 + 等距氛围。
 * 内容：问候 / 时间日签卡 / 核心指标；三天内截止 · 今日日程。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { pageTasksApi } from '@/apis/workbench/TaskApi'
import { pageEventsApi } from '@/apis/workbench/EventApi'
import { useUserStore } from '@/stores/userStore'
import BrandMarkView from '@/components/BrandMarkView.vue'
import {
  countTaskStatus,
  statusDoughnutOption,
  dueBarOption,
  completionTrendSeries,
  trendLineOption,
} from '@/utils/wbCharts'

const TASK_LIMIT = 6
const EVENT_LIMIT = 6
const DUE_WITHIN_DAYS = 3

const router = useRouter()
const userStore = useUserStore()
const now = ref(new Date())
const loading = ref(true)
const loadError = ref('')
const tasks = ref([])
const events = ref([])
let clockTimer = null

onMounted(() => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 250)
  refreshFocus()
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})

const displayName = computed(
  () => userStore.user?.username || userStore.user?.account || '伙伴',
)

const greeting = computed(() => {
  const h = now.value.getHours()
  if (h < 5) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const dateLabel = computed(() => {
  const d = dayjs(now.value)
  return `${d.format('YYYY.MM.DD')} · 周${WEEKDAYS[d.day()]}`
})

const weekdayLabel = computed(() => `周${WEEKDAYS[dayjs(now.value).day()]}`)

const timeLabel = computed(() => dayjs(now.value).format('HH:mm'))
const timeDigits = computed(() => {
  const d = dayjs(now.value)
  return {
    h: d.format('HH'),
    m: d.format('mm'),
    s: d.format('ss'),
  }
})
const clockHands = computed(() => {
  const d = now.value
  const s = d.getSeconds() + d.getMilliseconds() / 1000
  const m = d.getMinutes() + s / 60
  const h = (d.getHours() % 12) + m / 60
  const circ = 2 * Math.PI * 82
  return {
    hour: h * 30,
    minute: m * 6,
    second: s * 6,
    // 秒针进度弧：已走过的周长
    arcLen: (s / 60) * circ,
    arcCirc: circ,
  }
})
const dayNum = computed(() => dayjs(now.value).date())
const monthLabel = computed(() => `${dayjs(now.value).month() + 1}月`)

const today = computed(() => dayjs(now.value).startOf('day'))
const dueHorizon = computed(() => today.value.add(DUE_WITHIN_DAYS - 1, 'day').endOf('day'))

const focusTasks = computed(() => {
  const start = today.value
  const end = dueHorizon.value
  return tasks.value
    .filter((t) => {
      const status = String(t.status ?? '0')
      if (status === '2' || status === '3') return false
      if (!t.dueTime) return false
      const due = dayjs(t.dueTime)
      return due.isValid() && !due.isAfter(end)
    })
    .map((t) => {
      const daysLeft = dayjs(t.dueTime).startOf('day').diff(start, 'day')
      return { ...t, daysLeft, overdue: daysLeft < 0 }
    })
    .sort((a, b) => {
      if (a.overdue !== b.overdue) return a.overdue ? -1 : 1
      const dueDiff = dayjs(a.dueTime).valueOf() - dayjs(b.dueTime).valueOf()
      if (dueDiff !== 0) return dueDiff
      return Number(b.priority ?? 0) - Number(a.priority ?? 0)
    })
})

const visibleTasks = computed(() => focusTasks.value.slice(0, TASK_LIMIT))
const moreTaskCount = computed(() => Math.max(0, focusTasks.value.length - TASK_LIMIT))
const overdueCount = computed(() => focusTasks.value.filter((t) => t.overdue).length)

const todayEvents = computed(() => {
  const day = today.value
  const weekday = day.day() === 0 ? '7' : String(day.day())
  const dayKey = day.format('YYYY-MM-DD')
  return events.value
    .filter((ev) => eventOccursOn(ev, day, dayKey, weekday))
    .slice()
    .sort((a, b) => {
      const allA = Number(a.isAllDay) === 1 ? 0 : 1
      const allB = Number(b.isAllDay) === 1 ? 0 : 1
      if (allA !== allB) return allA - allB
      return dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf()
    })
})

const visibleEvents = computed(() => todayEvents.value.slice(0, EVENT_LIMIT))
const moreEventCount = computed(() => Math.max(0, todayEvents.value.length - EVENT_LIMIT))

const dayMarks = computed(() => {
  const labels = ['今', '明', '后']
  return [0, 1, 2].map((offset) => {
    const count =
      offset === 0
        ? focusTasks.value.filter((t) => t.daysLeft <= 0).length
        : focusTasks.value.filter((t) => t.daysLeft === offset).length
    return {
      key: offset,
      label: labels[offset],
      lit: count > 0,
      count,
    }
  })
})

const focusSummary = computed(() => {
  if (loading.value) return '正在整理今日重点…'
  if (loadError.value) return '数据暂时不可用，可稍后刷新'
  const taskN = focusTasks.value.length
  const eventN = todayEvents.value.length
  if (!taskN && !eventN) return '节奏轻松 — 没有临近截止或今日安排'
  const parts = []
  if (overdueCount.value) parts.push(`${overdueCount.value} 项已逾期`)
  else if (taskN) parts.push(`${taskN} 项将在三天内截止`)
  if (eventN) parts.push(`今日 ${eventN} 项日程`)
  return parts.join(' · ')
})

const coreMeters = computed(() => [
  {
    key: 'overdue',
    label: '逾期',
    value: loading.value ? '—' : String(overdueCount.value),
    hint: '需优先处理',
    tone: overdueCount.value > 0 ? 'danger' : 'idle',
  },
  {
    key: 'due',
    label: '三日内',
    value: loading.value ? '—' : String(focusTasks.value.length),
    hint: '临近截止',
    tone: focusTasks.value.length > 0 ? 'accent' : 'idle',
  },
  {
    key: 'events',
    label: '今日程',
    value: loading.value ? '—' : String(todayEvents.value.length),
    hint: '日程安排',
    tone: todayEvents.value.length > 0 ? 'accent' : 'idle',
  },
])

const statusChartOption = computed(() =>
  statusDoughnutOption(countTaskStatus(tasks.value), { title: 'TASK MIX' }),
)

const dueChartOption = computed(() => dueBarOption(dayMarks.value))

const weekTrend = computed(() => completionTrendSeries(tasks.value, today.value, 7))

const trendChartOption = computed(() =>
  trendLineOption(weekTrend.value.labels, weekTrend.value.values, { title: 'WEEK DONE' }),
)

const weekDoneTotal = computed(() => weekTrend.value.values.reduce((a, b) => a + b, 0))

function eventOccursOn(ev, day, dayKey, weekday) {
  const start = dayjs(ev.startTime)
  if (!start.isValid()) return false
  if (start.format('YYYY-MM-DD') === dayKey) return true
  if (String(ev.repeatType) !== '1') return false
  if (start.startOf('day').isAfter(day, 'day')) return false
  const days = String(ev.repeatWeekdays || '')
    .split(',')
    .map((d) => d.trim())
    .filter(Boolean)
  if (!days.length) return weekday === (start.day() === 0 ? '7' : String(start.day()))
  return days.includes(weekday)
}

function dueLabel(task) {
  if (task.overdue) {
    const n = Math.abs(task.daysLeft)
    return n === 1 ? '逾期 1 天' : `逾期 ${n} 天`
  }
  if (task.daysLeft === 0) return '今天截止'
  if (task.daysLeft === 1) return '明天截止'
  return `${task.daysLeft} 天后`
}

function formatDue(value) {
  const d = dayjs(value)
  if (!d.isValid()) return ''
  if (d.hour() === 0 && d.minute() === 0) return d.format('M/D')
  return d.format('M/D HH:mm')
}

function formatEventTime(ev) {
  if (Number(ev.isAllDay) === 1) return '全天'
  const s = dayjs(ev.startTime)
  if (!s.isValid()) return '—'
  return s.format('HH:mm')
}

async function refreshFocus() {
  loading.value = true
  loadError.value = ''
  try {
    const [taskRes, eventRes] = await Promise.all([
      pageTasksApi(1, null),
      pageEventsApi(1, null),
    ])
    tasks.value = taskRes?.data?.records || []
    events.value = eventRes?.data?.records || []
  } catch (err) {
    loadError.value = err?.message || '加载失败'
    tasks.value = []
    events.value = []
  } finally {
    loading.value = false
  }
}

function goTasks() {
  router.push('/task').catch(() => {})
}

function goEvents() {
  router.push('/event').catch(() => {})
}
</script>

<template>
  <div class="home">
    <!-- 氛围：对齐登录页 — 柔光 · 等距网格 · 数据流 · 颗粒 -->
    <div class="atm" aria-hidden="true">
      <div class="atm-glow atm-glow--a" />
      <div class="atm-glow atm-glow--b" />
      <div class="atm-iso" />
      <svg class="atm-flow" viewBox="0 0 1200 720" preserveAspectRatio="xMidYMid slice" fill="none">
        <path
          class="atm-flow__path"
          d="M60 540C240 400 340 600 500 440C660 280 780 540 980 360C1100 240 1160 300 1200 260"
          stroke="url(#homeFlowA)"
          stroke-width="1.4"
          stroke-linecap="round"
        />
        <path
          class="atm-flow__path atm-flow__path--b"
          d="M100 200C280 280 420 100 580 200C740 300 880 120 1060 180"
          stroke="url(#homeFlowB)"
          stroke-width="1.1"
          stroke-linecap="round"
        />
        <circle class="atm-flow__node" cx="500" cy="440" r="3.5" fill="rgba(34,211,238,0.55)" />
        <circle class="atm-flow__node atm-flow__node--b" cx="980" cy="360" r="3" fill="rgba(8,145,178,0.5)" />
        <defs>
          <linearGradient id="homeFlowA" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(8,145,178,0.03)" />
            <stop offset="50%" stop-color="rgba(34,211,238,0.32)" />
            <stop offset="100%" stop-color="rgba(8,145,178,0.05)" />
          </linearGradient>
          <linearGradient id="homeFlowB" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(14,116,144,0.04)" />
            <stop offset="55%" stop-color="rgba(8,145,178,0.2)" />
            <stop offset="100%" stop-color="rgba(34,211,238,0.05)" />
          </linearGradient>
        </defs>
      </svg>
      <div class="atm-wm">WB</div>
      <div class="atm-grain" />
    </div>

    <div class="shell">
      <header class="hero">
        <div class="hero-copy">
          <div class="hero-mast">
            <div class="hero-brand">
              <span class="hero-brand__mark"><BrandMarkView :size="26" /></span>
              <div class="hero-brand__text">
                <span class="hero-brand__name">Personal Workbench</span>
                <span class="hero-brand__tag">今日工作台</span>
              </div>
            </div>
            <p class="hero-kicker">
              <span class="hero-kicker__chip">
                <span class="hero-kicker__date">{{ dateLabel }}</span>
                <span class="hero-kicker__sep" aria-hidden="true" />
                <span class="hero-kicker__time">{{ timeLabel }}</span>
              </span>
            </p>
          </div>

          <h1 class="hero-title">
            <span class="hero-greet">{{ greeting }}，</span>
            <span class="hero-name">{{ displayName }}</span>
          </h1>
          <p class="hero-lead">{{ focusSummary }}</p>

          <ul class="meters" aria-label="今日核心指标">
            <li
              v-for="(m, idx) in coreMeters"
              :key="m.key"
              class="meter"
              :class="[`meter--${m.key}`, `meter--${m.tone}`]"
              :style="{ '--i': idx }"
            >
              <span class="meter-glyph" aria-hidden="true">
                <svg v-if="m.key === 'overdue'" viewBox="0 0 20 20" fill="none">
                  <path d="M10 3.2L17.5 16H2.5L10 3.2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
                  <path d="M10 8v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                  <circle cx="10" cy="14.2" r="1" fill="currentColor" />
                </svg>
                <svg v-else-if="m.key === 'due'" viewBox="0 0 20 20" fill="none">
                  <rect x="3.5" y="3.5" width="13" height="13" rx="2" stroke="currentColor" stroke-width="1.5" />
                  <path d="M7 10h6M7 13h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 20 20" fill="none">
                  <circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5" />
                  <path d="M10 6V10l3 1.8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
              </span>
              <span class="meter-body">
                <span class="meter-label">{{ m.label }}</span>
                <span class="meter-value">{{ m.value }}</span>
                <span class="meter-hint">{{ m.hint }}</span>
              </span>
            </li>
          </ul>

          <div class="hero-actions">
            <button type="button" class="btn-primary" :disabled="loading" @click="refreshFocus">
              <span>{{ loading ? '整理中…' : '刷新重点' }}</span>
              <span class="btn-primary__ico" aria-hidden="true">
                <svg viewBox="0 0 16 16" fill="none">
                  <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </button>
            <button type="button" class="btn-text" @click="goTasks">打开事务</button>
            <button type="button" class="btn-text" @click="goEvents">打开日程</button>
          </div>
        </div>

        <aside class="timecard" :aria-label="`现在 ${timeDigits.h}:${timeDigits.m}:${timeDigits.s}，${monthLabel}${dayNum}日`">
          <div class="timecard-plate">
            <div class="timecard-aura" aria-hidden="true" />
            <div class="timecard-grain" aria-hidden="true" />

            <header class="timecard-meta">
              <span class="timecard-meta__now">此刻</span>
              <span class="timecard-meta__dot" aria-hidden="true" />
              <span class="timecard-meta__week">{{ weekdayLabel }}</span>
            </header>

            <div class="timecard-stage">
              <div class="timecard-clock" role="timer" aria-hidden="true">
                <svg class="timecard-face" viewBox="0 0 200 200">
                  <defs>
                    <linearGradient id="clockRing" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="rgba(34,211,238,0.55)" />
                      <stop offset="100%" stop-color="rgba(8,145,178,0.08)" />
                    </linearGradient>
                    <linearGradient id="clockArc" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="rgba(34,211,238,0.95)" />
                      <stop offset="100%" stop-color="rgba(8,145,178,0.55)" />
                    </linearGradient>
                    <radialGradient id="clockPad" cx="42%" cy="32%" r="68%">
                      <stop offset="0%" stop-color="rgba(34,211,238,0.3)" />
                      <stop offset="55%" stop-color="rgba(34,211,238,0.06)" />
                      <stop offset="100%" stop-color="rgba(255,255,255,0)" />
                    </radialGradient>
                  </defs>
                  <circle cx="100" cy="100" r="92" fill="url(#clockPad)" />
                  <circle cx="100" cy="100" r="88" fill="none" stroke="url(#clockRing)" stroke-width="1.25" opacity="0.85" />
                  <circle cx="100" cy="100" r="74" fill="none" stroke="currentColor" stroke-width="0.8" opacity="0.12" />
                  <circle
                    class="timecard-arc"
                    cx="100" cy="100" r="82"
                    fill="none"
                    stroke="url(#clockArc)"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    :stroke-dasharray="`${clockHands.arcLen} ${clockHands.arcCirc}`"
                    transform="rotate(-90 100 100)"
                  />
                  <g class="timecard-ticks" stroke="currentColor">
                    <line
                      v-for="i in 60"
                      :key="`t${i}`"
                      x1="100"
                      y1="18"
                      x2="100"
                      :y2="(i - 1) % 5 === 0 ? 30 : 24"
                      :stroke-width="(i - 1) % 5 === 0 ? 1.8 : 0.9"
                      :opacity="(i - 1) % 5 === 0 ? 0.7 : 0.18"
                      :transform="`rotate(${(i - 1) * 6} 100 100)`"
                    />
                  </g>
                  <g>
                    <line
                      class="timecard-hand timecard-hand--h"
                      x1="100" y1="100" x2="100" y2="56"
                      stroke="currentColor" stroke-width="3.4" stroke-linecap="round"
                      :transform="`rotate(${clockHands.hour} 100 100)`"
                    />
                    <line
                      class="timecard-hand timecard-hand--m"
                      x1="100" y1="100" x2="100" y2="38"
                      stroke="currentColor" stroke-width="2.1" stroke-linecap="round"
                      :transform="`rotate(${clockHands.minute} 100 100)`"
                    />
                    <line
                      class="timecard-hand timecard-hand--s"
                      x1="100" y1="118" x2="100" y2="28"
                      stroke-width="1.25" stroke-linecap="round"
                      :transform="`rotate(${clockHands.second} 100 100)`"
                    />
                    <circle class="timecard-hub" cx="100" cy="100" r="5" />
                    <circle cx="100" cy="100" r="2.2" fill="#fff" />
                  </g>
                </svg>
              </div>

              <div class="timecard-cal" aria-hidden="true">
                <span class="timecard-month">{{ monthLabel }}</span>
                <span class="timecard-day">{{ dayNum }}</span>
                <span class="timecard-cal__rule" />
                <span class="timecard-cal__hint">日签</span>
              </div>
            </div>

            <p class="timecard-digits" aria-hidden="true">
              <span class="timecard-unit">{{ timeDigits.h }}</span>
              <span class="timecard-colon">:</span>
              <span class="timecard-unit">{{ timeDigits.m }}</span>
              <span class="timecard-colon timecard-colon--soft">:</span>
              <span class="timecard-unit timecard-unit--sec">{{ timeDigits.s }}</span>
            </p>

            <div class="timecard-rail" aria-hidden="true">
              <template v-for="(m, idx) in dayMarks" :key="m.key">
                <span
                  class="timecard-rail__item"
                  :class="{ 'is-lit': m.lit }"
                  :title="m.lit ? `${m.count} 项` : undefined"
                >
                  <span class="timecard-rail__node" />
                  <span class="timecard-rail__label">{{ m.label }}</span>
                </span>
                <span
                  v-if="idx < dayMarks.length - 1"
                  class="timecard-rail__seg"
                  :class="{ 'is-on': m.lit }"
                />
              </template>
            </div>
          </div>
        </aside>
      </header>

      <section class="insights" aria-label="进度洞察" :aria-busy="loading">
        <article class="insight insight--mix" style="--i: 0">
          <v-chart class="insight-chart" :option="statusChartOption" autoresize />
          <p class="insight-foot">
            <span class="insight-foot__label">全部任务</span>
            <span class="insight-foot__val">{{ loading ? '—' : tasks.length }}</span>
          </p>
        </article>
        <article class="insight insight--due" style="--i: 1">
          <v-chart class="insight-chart" :option="dueChartOption" autoresize />
          <p class="insight-foot">
            <span class="insight-foot__label">三日内压力</span>
            <span class="insight-foot__val">{{ loading ? '—' : focusTasks.length }}</span>
          </p>
        </article>
        <article class="insight insight--trend" style="--i: 2">
          <v-chart class="insight-chart" :option="trendChartOption" autoresize />
          <p class="insight-foot">
            <span class="insight-foot__label">本周完成</span>
            <span class="insight-foot__val">{{ loading ? '—' : weekDoneTotal }}</span>
          </p>
        </article>
      </section>

      <div class="boards" :aria-busy="loading">
        <section class="pane pane--tasks" aria-labelledby="pane-tasks">
          <div class="pane-rail" aria-hidden="true" />
          <header class="pane-head">
            <div class="pane-head__left">
              <span class="pane-ico pane-ico--check" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <rect x="4" y="4" width="16" height="16" rx="2" stroke="currentColor" stroke-width="1.6" />
                  <path d="M8 12l2.8 2.8L16.5 9" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <div>
                <p class="pane-eyebrow">
                  <span class="pane-eyebrow__en">Task</span>
                  <span class="pane-eyebrow__zh">清单</span>
                </p>
                <h2 id="pane-tasks" class="pane-title">三天内截止</h2>
              </div>
            </div>
            <button type="button" class="pane-link" @click="goTasks">全部</button>
          </header>

          <div v-if="loading" class="skel" aria-hidden="true">
            <div v-for="n in 3" :key="n" class="skel-row" />
          </div>

          <div v-else-if="!visibleTasks.length" class="empty empty--tasks">
            <div class="empty-stack" aria-hidden="true">
              <span class="empty-row"><i class="empty-box" /><i class="empty-line" /></span>
              <span class="empty-row is-done"><i class="empty-box is-checked" /><i class="empty-line empty-line--short" /></span>
              <span class="empty-row"><i class="empty-box" /><i class="empty-line empty-line--mid" /></span>
            </div>
            <p class="empty-title">没有临近截止</p>
            <p class="empty-desc">近三天没有未完成的到期任务。</p>
            <button type="button" class="btn-ghost" @click="goTasks">去事务</button>
          </div>

          <ul v-else class="list list--tasks" role="list">
            <li
              v-for="(task, idx) in visibleTasks"
              :key="task.taskId"
              class="card card--task"
              :class="{ 'is-overdue': task.overdue, 'is-today': !task.overdue && task.daysLeft === 0 }"
              :style="{ '--i': idx }"
            >
              <button type="button" class="card-btn" @click="goTasks">
                <span class="card-check" aria-hidden="true" />
                <span class="card-main">
                  <span class="card-title">{{ task.title || '未命名任务' }}</span>
                  <span class="card-meta">
                    <span class="card-badge" :class="{ 'card-badge--warn': task.overdue }">{{ dueLabel(task) }}</span>
                    <time :datetime="task.dueTime">{{ formatDue(task.dueTime) }}</time>
                  </span>
                </span>
              </button>
            </li>
          </ul>

          <p v-if="moreTaskCount > 0" class="more">
            另有 {{ moreTaskCount }} 项
            <button type="button" class="btn-text" @click="goTasks">查看</button>
          </p>
        </section>

        <section class="pane pane--events" aria-labelledby="pane-events">
          <header class="pane-banner">
            <div class="pane-banner__left">
              <span class="pane-ico pane-ico--clock" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="8.5" stroke="currentColor" stroke-width="1.6" />
                  <path d="M12 7.5V12l3.2 2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <div>
                <p class="pane-eyebrow pane-eyebrow--on-dark">
                  <span class="pane-eyebrow__en">Time</span>
                  <span class="pane-eyebrow__zh">时刻</span>
                </p>
                <h2 id="pane-events" class="pane-title">日程安排</h2>
              </div>
            </div>
            <button type="button" class="pane-link pane-link--on-dark" @click="goEvents">全部</button>
          </header>

          <div class="pane-body">
            <div v-if="loading" class="skel skel--agenda" aria-hidden="true">
              <div v-for="n in 3" :key="`e${n}`" class="skel-row skel-row--agenda" />
            </div>

            <div v-else-if="!visibleEvents.length" class="empty empty--events">
              <div class="empty-clock" aria-hidden="true">
                <span class="empty-clock__ring" />
                <span class="empty-clock__hand empty-clock__hand--h" />
                <span class="empty-clock__hand empty-clock__hand--m" />
                <span class="empty-clock__core" />
              </div>
              <p class="empty-title">今天暂无安排</p>
              <p class="empty-desc">可以把时间留给手头的任务。</p>
              <button type="button" class="btn-ghost btn-ghost--pill" @click="goEvents">去日程</button>
            </div>

            <ul v-else class="list list--agenda" role="list">
              <li
                v-for="(ev, idx) in visibleEvents"
                :key="ev.eventId"
                class="card card--event"
                :style="{ '--i': idx }"
              >
                <button type="button" class="card-btn card-btn--event" @click="goEvents">
                  <time class="card-clock">{{ formatEventTime(ev) }}</time>
                  <span class="card-rail" aria-hidden="true">
                    <span class="card-dot" />
                  </span>
                  <span class="card-main">
                    <span class="card-title">{{ ev.title || '未命名日程' }}</span>
                    <span v-if="ev.location" class="card-meta">{{ ev.location }}</span>
                  </span>
                </button>
              </li>
            </ul>

            <p v-if="moreEventCount > 0" class="more">
              另有 {{ moreEventCount }} 项
              <button type="button" class="btn-text" @click="goEvents">查看</button>
            </p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  --ink: var(--color-text-primary);
  --body: var(--color-text-body);
  --muted: var(--color-text-secondary);
  --line: var(--color-border);
  --line-strong: var(--color-border-strong);
  --accent: var(--color-accent);
  --deep: var(--color-accent-deep);
  --light: var(--color-accent-light);
  --danger: var(--color-red);
  --font-brand: 'Outfit', 'Noto Sans SC', var(--font-family-sans);
  --font-display: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', serif;
  --font-body: 'Noto Sans SC', 'Outfit', var(--font-family-sans);
  --ease: cubic-bezier(0.16, 1, 0.3, 1);

  position: relative;
  box-sizing: border-box;
  min-height: 100%;
  padding: 22px 22px 44px;
  font-family: var(--font-body);
  color: var(--ink);
  overflow: auto;
}

/* ── Atmosphere（对齐登录页，克制） ── */
.atm {
  pointer-events: none;
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

.atm-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(72px);
}

.atm-glow--a {
  width: 460px;
  height: 380px;
  top: -140px;
  right: 2%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.3), transparent 70%);
  animation: drift 16s var(--ease) infinite alternate;
}

.atm-glow--b {
  width: 320px;
  height: 320px;
  bottom: -40px;
  left: -60px;
  background: radial-gradient(circle, rgba(14, 116, 144, 0.18), transparent 72%);
  animation: drift 20s var(--ease) infinite alternate-reverse;
}

.atm-iso {
  position: absolute;
  inset: -8% -4%;
  opacity: 0.28;
  background-image:
    repeating-linear-gradient(
      30deg,
      transparent,
      transparent 67px,
      rgba(8, 145, 178, 0.07) 67px,
      rgba(8, 145, 178, 0.07) 68px
    ),
    repeating-linear-gradient(
      150deg,
      transparent,
      transparent 67px,
      rgba(8, 145, 178, 0.05) 67px,
      rgba(8, 145, 178, 0.05) 68px
    );
  transform: skewY(-4deg);
  mask-image: radial-gradient(ellipse 75% 65% at 70% 20%, #000 15%, transparent 72%);
}

.atm-flow {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.75;
}

.atm-flow__path {
  stroke-dasharray: 6 12;
  animation: dash 32s linear infinite;
}

.atm-flow__path--b {
  animation-duration: 40s;
  animation-direction: reverse;
}

.atm-flow__node {
  animation: pulse-node 3.8s var(--ease) infinite;
}

.atm-flow__node--b {
  animation-delay: 1s;
}

.atm-wm {
  position: absolute;
  right: 4%;
  bottom: 18%;
  top: auto;
  font-family: var(--font-brand);
  font-size: clamp(64px, 10vw, 110px);
  font-weight: 800;
  letter-spacing: -0.08em;
  line-height: 1;
  color: rgba(8, 145, 178, 0.045);
  user-select: none;
}

.atm-grain {
  position: absolute;
  inset: 0;
  opacity: 0.22;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

@keyframes drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(-14px, 12px) scale(1.05); }
}

@keyframes dash {
  to { stroke-dashoffset: -220; }
}

@keyframes pulse-node {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.4); }
}

.shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: none;
  margin: 0;
}

/* ── Hero：左文案 + 右日签合成一块，消除中间空洞 ── */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 320px);
  gap: 0;
  align-items: stretch;
  margin-bottom: 24px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 28px;
  background:
    radial-gradient(ellipse 55% 70% at 8% 0%, rgba(34, 211, 238, 0.14), transparent 55%),
    radial-gradient(ellipse 40% 50% at 100% 100%, rgba(8, 145, 178, 0.08), transparent 50%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.78), rgba(238, 248, 252, 0.62));
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.9) inset,
    0 14px 36px rgba(8, 145, 178, 0.08);
  backdrop-filter: blur(16px) saturate(1.1);
  overflow: hidden;
  animation: rise 0.65s var(--ease) both;
}

.hero-copy {
  min-width: 0;
  padding: 24px 28px 26px;
  display: flex;
  flex-direction: column;
}

.hero-mast {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 12px 14px;
  margin-bottom: 18px;
}

.hero-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--accent);
}

.hero-brand__mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(238, 248, 252, 0.85));
  border: 1px solid rgba(8, 145, 178, 0.18);
  box-shadow:
    0 1px 2px rgba(8, 116, 146, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.hero-brand__text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.hero-brand__name {
  font-family: var(--font-brand);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.15;
  color: var(--ink);
}

.hero-brand__tag {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--muted);
}

.hero-kicker {
  margin: 0;
}

.hero-kicker__chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--body);
  letter-spacing: 0.02em;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(8, 145, 178, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
}

.hero-kicker__sep {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.4);
}

.hero-kicker__time {
  font-family: var(--font-family-mono);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1em;
  color: var(--muted);
}

.hero-title {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: clamp(1.85rem, 1.35rem + 1.6vw, 2.55rem);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.03em;
  text-wrap: balance;
}

.hero-greet {
  font-weight: 600;
  color: var(--ink);
}

.hero-name {
  background: linear-gradient(118deg, var(--deep) 6%, var(--accent) 52%, var(--light) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-lead {
  margin: 0;
  max-width: 40em;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.65;
  color: var(--body);
  text-wrap: pretty;
}

/* Meters — 拉满左栏宽度 */
.meters {
  list-style: none;
  margin: 22px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: none;
  flex: 0 0 auto;
}

.meter {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 14px 13px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.94), rgba(238, 248, 252, 0.72));
  box-shadow:
    0 1px 2px rgba(8, 116, 146, 0.04),
    0 8px 18px rgba(8, 145, 178, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(14px);
  animation: rise 0.55s var(--ease) both;
  transition: transform 220ms var(--ease), box-shadow 220ms var(--ease), border-color 220ms ease;
  overflow: hidden;
}

.meter::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 2.5px;
  border-radius: 0 2px 2px 0;
  background: rgba(8, 145, 178, 0.22);
}

.meter--overdue::before {
  background: linear-gradient(180deg, #f07167, var(--danger));
}

.meter--due::before {
  background: linear-gradient(180deg, var(--light), var(--accent));
}

.meter--events::before {
  background: linear-gradient(180deg, var(--accent), var(--deep));
}

.meter:hover {
  transform: translateY(-2px);
  border-color: rgba(8, 145, 178, 0.22);
  box-shadow:
    0 4px 8px rgba(8, 116, 146, 0.06),
    0 14px 28px rgba(8, 145, 178, 0.1),
    inset 0 1px 0 #fff;
}

.meter--danger {
  background:
    linear-gradient(165deg, rgba(255, 248, 247, 0.96), rgba(255, 240, 238, 0.82));
  border-color: rgba(224, 85, 69, 0.18);
}

.meter--danger .meter-glyph,
.meter--danger .meter-value {
  color: var(--danger);
}

.meter--danger .meter-glyph {
  background: var(--color-red-soft);
  border-color: rgba(224, 85, 69, 0.22);
}

.meter-glyph {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  margin-left: 4px;
  color: var(--deep);
  background: rgba(8, 145, 178, 0.08);
  border: 1px solid rgba(8, 145, 178, 0.14);
  border-radius: 11px;
  transition: background 200ms ease, border-color 200ms ease;
}

.meter--overdue .meter-glyph { border-radius: 8px; }
.meter--events .meter-glyph { border-radius: 50%; }

.meter:hover .meter-glyph {
  background: rgba(8, 145, 178, 0.12);
  border-color: rgba(8, 145, 178, 0.24);
}

.meter-glyph svg {
  width: 16px;
  height: 16px;
}

.meter-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 1px;
}

.meter-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.meter-value {
  font-family: var(--font-family-mono);
  font-size: 1.6rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.05em;
  line-height: 1.12;
  color: var(--deep);
}

.meter-hint {
  font-size: 12px;
  color: var(--muted);
  opacity: 0.92;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 16px;
  margin-top: auto;
  padding-top: 22px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  padding: 0 7px 0 18px;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #f0f9ff;
  background: linear-gradient(135deg, var(--light), var(--accent) 48%, var(--deep));
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.25) inset,
    0 6px 18px rgba(8, 145, 178, 0.32);
  cursor: pointer;
  transition:
    transform 180ms var(--ease),
    box-shadow 280ms var(--ease),
    filter 180ms ease;
}

.btn-primary__ico {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  transition: transform 200ms var(--ease);
}

.btn-primary__ico svg {
  width: 14px;
  height: 14px;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 26px rgba(8, 145, 178, 0.44);
  filter: brightness(1.03);
}

.btn-primary:hover:not(:disabled) .btn-primary__ico {
  transform: translateX(2px);
}

.btn-primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-text {
  border: none;
  background: none;
  padding: 8px 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--deep);
  cursor: pointer;
  text-decoration: underline;
  text-decoration-color: rgba(8, 145, 178, 0.35);
  text-underline-offset: 4px;
  transition: color 180ms ease, text-decoration-color 180ms ease;
}

.btn-text:hover {
  color: var(--accent);
  text-decoration-color: var(--accent);
}

.btn-primary:focus-visible,
.btn-text:focus-visible,
.btn-ghost:focus-visible,
.pane-link:focus-visible,
.card-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ── Timecard — 贴合 hero 右栏，铺满高度 ── */
.timecard {
  display: flex;
  justify-content: stretch;
  align-items: stretch;
  color: var(--accent);
  min-height: 100%;
  border-left: 1px solid rgba(8, 145, 178, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.45), rgba(232, 246, 251, 0.35));
  animation: rise 0.7s var(--ease) 0.12s both;
}

.timecard-plate {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  padding: 18px 18px 16px;
  border-radius: 0;
  border: none;
  background:
    radial-gradient(ellipse 80% 55% at 22% 18%, rgba(34, 211, 238, 0.2), transparent 55%),
    radial-gradient(ellipse 50% 40% at 92% 88%, rgba(8, 145, 178, 0.08), transparent 60%),
    transparent;
  box-shadow: none;
  backdrop-filter: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  transition: background 480ms var(--ease);
}

.hero:hover .timecard-plate {
  transform: none;
  background:
    radial-gradient(ellipse 80% 55% at 22% 18%, rgba(34, 211, 238, 0.26), transparent 55%),
    radial-gradient(ellipse 50% 40% at 92% 88%, rgba(8, 145, 178, 0.1), transparent 60%),
    transparent;
}

.timecard-aura {
  pointer-events: none;
  position: absolute;
  width: 160px;
  height: 160px;
  top: 42px;
  left: 10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.28), transparent 68%);
  filter: blur(18px);
  z-index: 0;
}

.timecard-grain {
  pointer-events: none;
  position: absolute;
  inset: 0;
  opacity: 0.18;
  mix-blend-mode: soft-light;
  z-index: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.timecard-meta {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-brand);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--muted);
}

.timecard-meta__now {
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--deep);
}

.timecard-meta__dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.4);
}

.timecard-meta__week {
  margin-left: auto;
  letter-spacing: 0.08em;
}

.timecard-stage {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 4px 10px;
  align-items: center;
  min-height: 148px;
  flex: 1 1 auto;
}

.timecard-clock {
  position: relative;
}

.timecard-face {
  width: 100%;
  aspect-ratio: 1;
  color: var(--deep);
  display: block;
  filter: drop-shadow(0 6px 14px rgba(8, 145, 178, 0.14));
}

.timecard-arc {
  transition: none;
}

.timecard-hand--h {
  opacity: 0.92;
}

.timecard-hand--m {
  opacity: 0.78;
}

.timecard-hand--s {
  stroke: var(--accent);
}

.timecard-hub {
  fill: var(--accent);
}

.timecard-cal {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 4px 0 4px 4px;
  min-width: 0;
}

.timecard-month {
  font-family: var(--font-brand);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--deep);
  opacity: 0.75;
}

.timecard-day {
  font-family: var(--font-family-mono);
  font-size: clamp(52px, 6.2vw, 64px);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.08em;
  line-height: 0.86;
  margin: 2px 0 8px;
  background: linear-gradient(185deg, var(--ink) 10%, var(--deep) 78%, var(--accent) 120%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.timecard-cal__rule {
  display: block;
  width: 28px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--accent), rgba(8, 145, 178, 0.1));
  margin-bottom: 6px;
}

.timecard-cal__hint {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--muted);
}

.timecard-digits {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
  margin: 0;
  padding: 2px 0 0;
  font-family: var(--font-family-mono);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.timecard-unit {
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--deep);
}

.timecard-unit--sec {
  font-size: 1.15rem;
  font-weight: 600;
  color: var(--accent);
  letter-spacing: 0.04em;
  min-width: 1.6em;
}

.timecard-colon {
  font-size: 1.35rem;
  font-weight: 600;
  color: rgba(8, 145, 178, 0.45);
  animation: colon-blink 1s steps(1) infinite;
  padding: 0 1px;
}

.timecard-colon--soft {
  opacity: 0.55;
  animation: none;
  font-size: 1.05rem;
  color: rgba(8, 145, 178, 0.35);
}

@keyframes colon-blink {
  0%, 49% { opacity: 0.7; }
  50%, 100% { opacity: 0.22; }
}

.timecard-rail {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  gap: 0;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(8, 145, 178, 0.1);
}

.timecard-rail__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 36px;
}

.timecard-rail__node {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1.5px solid rgba(8, 145, 178, 0.28);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.04);
  transition: background 220ms ease, border-color 220ms ease, box-shadow 220ms ease, transform 220ms var(--ease);
}

.timecard-rail__label {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
  letter-spacing: 0.06em;
  transition: color 220ms ease;
}

.timecard-rail__item.is-lit .timecard-rail__node {
  background: var(--accent);
  border-color: var(--deep);
  box-shadow: 0 0 0 4px rgba(34, 211, 238, 0.18);
  transform: scale(1.08);
}

.timecard-rail__item.is-lit .timecard-rail__label {
  color: var(--deep);
}

.timecard-rail__seg {
  flex: 1;
  height: 2px;
  min-width: 28px;
  max-width: 48px;
  margin-top: 3px;
  border-radius: 2px;
  background: rgba(8, 145, 178, 0.12);
  transition: background 220ms ease;
}

.timecard-rail__seg.is-on {
  background: linear-gradient(90deg, var(--accent), rgba(8, 145, 178, 0.25));
}

/* ── Insights (ECharts band) ── */
.insights {
  display: grid;
  grid-template-columns: 0.92fr 0.92fr 1.16fr;
  gap: 18px;
  margin-bottom: 22px;
  animation: rise 0.7s var(--ease) 0.08s both;
}

.insight {
  position: relative;
  overflow: hidden;
  padding: 16px 16px 12px;
  border-radius: var(--radius-panel, 20px);
  border: 1px solid rgba(255, 255, 255, 0.75);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.94), rgba(246, 251, 253, 0.88));
  box-shadow:
    var(--shadow-md),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(14px);
  animation: rise 0.65s var(--ease) calc(0.12s + var(--i, 0) * 0.06s) both;
}

.insight::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--light), var(--accent) 50%, var(--deep));
  opacity: 0.85;
}

.insight--trend::before {
  background: linear-gradient(180deg, var(--light), var(--deep));
}

.insight-chart {
  width: 100%;
  height: 210px;
}

.insight-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin: 2px 4px 0;
  padding-top: 8px;
  border-top: 1px solid rgba(8, 145, 178, 0.08);
}

.insight-foot__label {
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--muted);
  font-family: var(--font-brand);
}

.insight-foot__val {
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  color: var(--deep);
  font-family: var(--font-brand);
}

/* ── Boards ── */
.boards {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 22px;
  align-items: stretch;
  animation: rise 0.7s var(--ease) 0.14s both;
}

.pane {
  position: relative;
  min-height: 420px;
  overflow: hidden;
}

.pane--tasks {
  padding: 26px 24px 28px 28px;
  border-radius: var(--radius-panel);
  border: 1px solid rgba(255, 255, 255, 0.75);
  background:
    linear-gradient(165deg, rgba(255, 255, 255, 0.94), rgba(246, 251, 253, 0.9));
  box-shadow:
    var(--shadow-md),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(14px);
}

.pane-rail {
  position: absolute;
  left: 0;
  top: 18px;
  bottom: 18px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--light), var(--accent) 45%, var(--deep));
}

.pane--events {
  display: flex;
  flex-direction: column;
  padding: 0;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--line-strong);
  background: var(--color-surface);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.pane-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px 18px;
  background: linear-gradient(
    118deg,
    var(--color-zone-topbar-from),
    var(--color-zone-topbar-mid) 55%,
    var(--color-zone-topbar-to)
  );
  color: var(--color-zone-topbar-text);
  box-shadow: var(--shadow-zone-topbar);
}

.pane-banner__left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.pane-body {
  position: relative;
  flex: 1;
  padding: 22px 24px 28px;
  min-height: 0;
  background:
    radial-gradient(circle at 100% 0%, rgba(8, 145, 178, 0.06), transparent 45%),
    #fff;
}

.pane--tasks > :not(.pane-rail) {
  position: relative;
  z-index: 1;
}

.pane-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.pane-head__left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pane-eyebrow {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 6px;
  line-height: 1;
}

.pane-eyebrow__en {
  font-family: var(--font-brand);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--deep);
}

.pane-eyebrow__zh {
  font-size: 12px;
  font-weight: 600;
  color: var(--body);
}

.pane-eyebrow--on-dark .pane-eyebrow__en,
.pane-eyebrow--on-dark .pane-eyebrow__zh {
  color: rgba(240, 249, 255, 0.88);
}

.pane-ico {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
}

.pane-ico--check {
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(145deg, var(--accent), var(--deep));
  box-shadow: var(--shadow-accent);
}

.pane-ico--clock {
  border-radius: 50%;
  color: var(--deep);
  background: #fff;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.35);
}

.pane-ico svg {
  width: 20px;
  height: 20px;
}

.pane-title {
  margin: 0;
  font-size: 1.28rem;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--ink);
}

.pane-banner .pane-title {
  color: var(--color-zone-topbar-text);
  font-family: var(--font-display);
  font-weight: 600;
}

.pane-link {
  border: none;
  background: none;
  padding: 8px 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--body);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 180ms ease;
}

.pane-link:hover {
  color: var(--accent);
}

.pane-link--on-dark {
  color: rgba(240, 249, 255, 0.88);
  text-decoration-color: rgba(240, 249, 255, 0.4);
}

.pane-link--on-dark:hover {
  color: #fff;
}

.btn-ghost {
  height: 38px;
  padding: 0 16px;
  border: 1.5px solid var(--line-strong);
  border-radius: var(--radius-control);
  background: linear-gradient(180deg, #fff, var(--color-surface-muted));
  color: var(--deep);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 180ms var(--ease), border-color 180ms ease, box-shadow 180ms ease;
}

.btn-ghost:hover {
  border-color: rgba(8, 145, 178, 0.45);
  box-shadow: var(--shadow-xs);
  transform: translateY(-1px);
}

.btn-ghost:active {
  transform: scale(0.98);
}

.btn-ghost--pill {
  border-radius: 999px;
}

/* Cards */
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list--tasks {
  gap: 8px;
}

.list--agenda {
  gap: 0;
}

.card {
  animation: rise 0.5s var(--ease) both;
}

.card-btn {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 14px;
  border: 1px solid transparent;
  text-align: left;
  cursor: pointer;
  transition:
    transform 200ms var(--ease),
    background 200ms ease,
    border-color 200ms ease,
    box-shadow 200ms ease;
}

.card--task .card-btn {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--line-strong);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.card--task .card-btn:hover {
  border-color: rgba(8, 145, 178, 0.4);
  box-shadow: var(--shadow-xs), inset 0 1px 0 #fff;
  transform: translateX(3px);
}

.card-btn:active {
  transform: scale(0.99);
}

.card.is-today .card-btn {
  background: var(--color-accent-soft);
  border-color: rgba(8, 145, 178, 0.35);
}

.card.is-overdue .card-btn {
  background:
    repeating-linear-gradient(
      -45deg,
      #fff,
      #fff 4px,
      var(--color-red-soft) 4px,
      var(--color-red-soft) 8px
    );
  border-color: rgba(224, 85, 69, 0.4);
  border-style: dashed;
}

.card-check {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin-top: 2px;
  border-radius: 6px;
  border: 2px solid var(--deep);
  background: #fff;
}

.card.is-today .card-check {
  background: var(--accent);
  border-color: var(--deep);
  box-shadow: inset 0 0 0 3px #fff;
}

.card.is-overdue .card-check {
  border-style: dashed;
  border-color: var(--danger);
}

.card-btn--event {
  align-items: stretch;
  gap: 0;
  padding: 12px 12px 12px 0;
  border-radius: 0;
  background: transparent;
  border: none;
}

.card-btn--event:hover {
  background: rgba(238, 248, 252, 0.65);
  transform: none;
  border-radius: 12px;
}

.card-rail {
  position: relative;
  flex-shrink: 0;
  width: 28px;
  display: flex;
  justify-content: center;
}

.card-rail::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  margin-left: -1px;
  background: linear-gradient(180deg, var(--accent), rgba(8, 145, 178, 0.12));
}

.card--event:first-child .card-rail::before {
  top: 14px;
}

.card--event:last-child .card-rail::before {
  bottom: calc(100% - 14px);
}

.card-dot {
  position: relative;
  z-index: 1;
  width: 12px;
  height: 12px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--accent);
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px var(--deep);
}

.card-clock {
  flex-shrink: 0;
  width: 3.5rem;
  padding-top: 4px;
  font-family: var(--font-family-mono);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--deep);
  text-align: right;
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 1px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--body);
  font-variant-numeric: tabular-nums;
}

.card-badge {
  display: inline-flex;
  padding: 2px 9px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12px;
  color: var(--deep);
  background: var(--color-accent-soft);
  border: 1px solid var(--line-strong);
}

.card-badge--warn,
.card.is-overdue .card-badge {
  color: #9a3d32;
  border-color: rgba(224, 85, 69, 0.35);
  background: var(--color-red-soft);
}

.card.is-today .card-badge {
  background: var(--deep);
  color: #fff;
  border-color: var(--deep);
}

/* Empty */
.empty {
  padding: 28px 8px 12px;
}

.empty--tasks {
  text-align: left;
}

.empty--events {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: 40px;
}

.empty-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(100%, 200px);
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 12px;
  border: 1.5px dashed rgba(8, 145, 178, 0.4);
  background: var(--color-surface-muted);
}

.empty-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.empty-box {
  width: 15px;
  height: 15px;
  border-radius: 4px;
  border: 2px solid var(--deep);
  flex-shrink: 0;
  background: #fff;
}

.empty-box.is-checked {
  background: var(--accent);
  border-color: var(--deep);
  box-shadow: inset 0 0 0 2px #fff;
}

.empty-line {
  flex: 1;
  height: 5px;
  border-radius: 1px;
  background: rgba(8, 145, 178, 0.2);
}

.empty-line--mid { width: 70%; flex: 0 1 70%; }
.empty-line--short { width: 48%; flex: 0 1 48%; }

.empty-clock {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 18px;
}

.empty-clock__ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid var(--deep);
  background: linear-gradient(160deg, #fff, var(--color-surface-muted));
  box-shadow: var(--shadow-sm), 0 0 0 6px rgba(8, 145, 178, 0.1);
}

.empty-clock__hand {
  position: absolute;
  left: 50%;
  top: 50%;
  background: var(--deep);
  border-radius: 1px;
}

.empty-clock__hand--h {
  width: 3px;
  height: 24px;
  margin-left: -1.5px;
  margin-top: -24px;
  transform: rotate(40deg);
  transform-origin: 50% 100%;
}

.empty-clock__hand--m {
  width: 2.5px;
  height: 32px;
  margin-left: -1.25px;
  margin-top: -32px;
  background: var(--accent);
  transform: rotate(130deg);
  transform-origin: 50% 100%;
}

.empty-clock__core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 8px;
  height: 8px;
  margin: -4px 0 0 -4px;
  border-radius: 50%;
  background: var(--accent);
}

.empty-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.empty-desc {
  margin: 0 0 14px;
  max-width: 22em;
  font-size: 14px;
  line-height: 1.55;
  color: var(--body);
}

.more {
  margin: 16px 2px 0;
  font-size: 13px;
  color: var(--body);
}

.skel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skel-row {
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(
    90deg,
    rgba(8, 145, 178, 0.05),
    rgba(8, 145, 178, 0.12),
    rgba(8, 145, 178, 0.05)
  );
  background-size: 200% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

.skel--agenda .skel-row--agenda {
  border-radius: 999px 14px 14px 999px;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
    margin-bottom: 22px;
  }

  .hero-copy {
    padding: 22px 20px 20px;
  }

  .timecard {
    border-left: none;
    border-top: 1px solid rgba(8, 145, 178, 0.12);
  }

  .timecard-plate {
    width: 100%;
    min-height: 0;
    padding: 18px 20px 20px;
  }

  .boards {
    grid-template-columns: 1fr;
  }

  .insights {
    grid-template-columns: 1fr 1fr;
  }

  .insight--trend {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .home {
    padding: 16px 12px 36px;
  }

  .hero {
    border-radius: 22px;
    margin-bottom: 18px;
  }

  .hero-copy {
    padding: 18px 16px 16px;
  }

  .hero-mast {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .meters {
    grid-template-columns: 1fr;
  }

  .insights {
    grid-template-columns: 1fr;
  }

  .insight--trend {
    grid-column: auto;
  }

  .pane {
    min-height: 0;
  }

  .pane--tasks {
    padding: 20px 16px 22px 20px;
  }

  .hero-title {
    font-size: 1.65rem;
  }

  .atm-wm {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero,
  .timecard,
  .boards,
  .insights,
  .insight,
  .card,
  .meter,
  .atm-glow,
  .atm-flow__path,
  .atm-flow__node,
  .timecard-arc,
  .skel-row,
  .btn-primary,
  .card-btn,
  .timecard-plate,
  .timecard-colon {
    animation: none !important;
    transition: none !important;
  }

  .hero:hover .timecard-plate {
    transform: none;
  }
}
</style>
