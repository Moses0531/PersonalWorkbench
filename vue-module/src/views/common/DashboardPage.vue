<script setup>
/**
 * 工作台首页
 * 构图：问候 + 核心指标 + 日签图案；内容：三天内截止 / 今日日程。
 * 氛围图案（流线 / 几何环）服务层次，不抢数据。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { pageTasksApi } from '@/apis/workbench/TaskApi'
import { pageEventsApi } from '@/apis/workbench/EventApi'
import { useUserStore } from '@/stores/userStore'
import BrandMarkView from '@/components/BrandMarkView.vue'

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
  }, 30000)
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

const dateLabel = computed(() => {
  const d = dayjs(now.value)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.format('YYYY.MM.DD')} · 周${weekdays[d.day()]}`
})

const timeLabel = computed(() => dayjs(now.value).format('HH:mm'))
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

/** 日签上的三日微标：图案元素，兼提示有无截止 */
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

/** 核心要素条：逾期 / 三日截止 / 今日日程 */
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
    <!-- 氛围层：光晕 · 网格 · 流线 · 几何环 · 颗粒 -->
    <div class="atm" aria-hidden="true">
      <div class="atm-glow atm-glow--a" />
      <div class="atm-glow atm-glow--b" />
      <div class="atm-glow atm-glow--c" />
      <div class="atm-hatch" />
      <svg class="atm-flow" viewBox="0 0 1200 720" preserveAspectRatio="xMidYMid slice" fill="none">
        <path
          class="atm-flow__path"
          d="M40 520C220 380 320 620 480 460C640 300 760 560 960 380C1080 260 1140 320 1180 280"
          stroke="url(#homeFlowA)"
          stroke-width="1.5"
          stroke-linecap="round"
        />
        <path
          class="atm-flow__path atm-flow__path--b"
          d="M80 180C260 260 400 80 560 180C720 280 860 100 1040 160"
          stroke="url(#homeFlowB)"
          stroke-width="1.2"
          stroke-linecap="round"
        />
        <circle class="atm-flow__node" cx="480" cy="460" r="3.5" fill="rgba(8,145,178,0.4)" />
        <circle class="atm-flow__node atm-flow__node--b" cx="960" cy="380" r="3" fill="rgba(34,211,238,0.5)" />
        <circle class="atm-flow__node atm-flow__node--c" cx="560" cy="180" r="2.5" fill="rgba(14,116,144,0.35)" />
        <defs>
          <linearGradient id="homeFlowA" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(8,145,178,0.04)" />
            <stop offset="50%" stop-color="rgba(34,211,238,0.28)" />
            <stop offset="100%" stop-color="rgba(8,145,178,0.06)" />
          </linearGradient>
          <linearGradient id="homeFlowB" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(14,116,144,0.05)" />
            <stop offset="55%" stop-color="rgba(8,145,178,0.18)" />
            <stop offset="100%" stop-color="rgba(34,211,238,0.06)" />
          </linearGradient>
        </defs>
      </svg>
      <div class="atm-rings atm-rings--a"><span /><span /><span /></div>
      <div class="atm-rings atm-rings--b"><span /><span /></div>
      <div class="atm-hex" />
      <div class="atm-grain" />
    </div>

    <div class="shell">
      <!-- 主视觉：文案 + 日签图案 -->
      <header class="hero">
        <div class="hero-copy">
          <div class="hero-brand">
            <span class="hero-brand__mark"><BrandMarkView :size="22" /></span>
            <span class="hero-brand__name">Personal Workbench</span>
            <span class="hero-brand__tick" />
          </div>

          <p class="hero-kicker">
            <span class="hero-kicker__date">{{ dateLabel }}</span>
            <span class="hero-kicker__sep" aria-hidden="true" />
            <span class="hero-kicker__time">{{ timeLabel }}</span>
          </p>
          <h1 class="hero-title">
            <span class="hero-greet">{{ greeting }}，</span>
            <span class="hero-name">{{ displayName }}</span>
          </h1>
          <p class="hero-lead">{{ focusSummary }}</p>

          <!-- 核心要素：逾期 / 三日截止 / 今日程 -->
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
                  <path d="M10 3.2L17.5 16H2.5L10 3.2Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
                  <path d="M10 8v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                  <circle cx="10" cy="14.2" r="1" fill="currentColor" />
                </svg>
                <svg v-else-if="m.key === 'due'" viewBox="0 0 20 20" fill="none">
                  <rect x="3.5" y="3.5" width="13" height="13" rx="2" stroke="currentColor" stroke-width="1.4" />
                  <path d="M7 10h6M7 13h4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 20 20" fill="none">
                  <circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.4" />
                  <path d="M10 6V10l3 1.8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
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
            <button type="button" class="btn-soft" :disabled="loading" @click="refreshFocus">
              {{ loading ? '整理中…' : '刷新重点' }}
            </button>
            <button type="button" class="btn-text" @click="goTasks">打开事务</button>
            <button type="button" class="btn-text" @click="goEvents">打开日程</button>
          </div>
        </div>

        <!-- 日签：首页主图案 -->
        <aside class="daycard" aria-hidden="true">
          <div class="daycard-orbit-ring" />
          <div class="daycard-plate">
            <div class="daycard-corner daycard-corner--tl" />
            <div class="daycard-corner daycard-corner--br" />
            <svg class="daycard-motif" viewBox="0 0 220 240" fill="none">
              <defs>
                <linearGradient id="dcFill" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="rgba(34,211,238,0.38)" />
                  <stop offset="100%" stop-color="rgba(8,145,178,0.08)" />
                </linearGradient>
                <linearGradient id="dcRing" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(8,145,178,0.5)" />
                  <stop offset="100%" stop-color="rgba(8,145,178,0.04)" />
                </linearGradient>
              </defs>
              <circle cx="110" cy="112" r="98" stroke="url(#dcRing)" stroke-width="1" opacity="0.75" />
              <circle
                class="daycard-orbit"
                cx="110"
                cy="112"
                r="80"
                stroke="currentColor"
                stroke-width="0.8"
                stroke-dasharray="3 8"
                opacity="0.3"
              />
              <circle
                class="daycard-orbit daycard-orbit--rev"
                cx="110"
                cy="112"
                r="62"
                stroke="currentColor"
                stroke-width="0.6"
                stroke-dasharray="1 6"
                opacity="0.2"
              />
              <!-- 品牌机匣 -->
              <path
                d="M110 48 L150 70 V126 L110 148 L70 126 V70 Z"
                fill="url(#dcFill)"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linejoin="round"
              />
              <path
                d="M110 48 V148 M70 70 L150 70 M70 126 L150 126 M70 70 L110 98 L150 70 M70 126 L110 98 L150 126"
                stroke="currentColor"
                stroke-width="1.15"
                stroke-linejoin="round"
                opacity="0.55"
              />
              <circle cx="110" cy="98" r="3.8" fill="currentColor" opacity="0.75" />
              <!-- 事务 / 日程小印 -->
              <g opacity="0.55" transform="translate(26 172)">
                <rect width="18" height="18" rx="3.5" stroke="currentColor" stroke-width="1.3" />
                <path d="M5 9.2l3 3L13.5 5.5" stroke="currentColor" stroke-width="1.55" stroke-linecap="round" fill="none" />
              </g>
              <g opacity="0.55" transform="translate(176 170)">
                <circle cx="9" cy="9" r="9" stroke="currentColor" stroke-width="1.3" />
                <path d="M9 4.5V9.2l3.2 2" stroke="currentColor" stroke-width="1.45" stroke-linecap="round" fill="none" />
              </g>
            </svg>

            <div class="daycard-num">
              <span class="daycard-month">{{ monthLabel }}</span>
              <span class="daycard-day">{{ dayNum }}</span>
            </div>

            <div class="daycard-marks">
              <span
                v-for="m in dayMarks"
                :key="m.key"
                class="daycard-mark"
                :class="{ 'is-lit': m.lit }"
                :title="m.lit ? `${m.count} 项` : undefined"
              >
                {{ m.label }}
                <i v-if="m.lit" class="daycard-mark__dot" />
              </span>
            </div>
          </div>
        </aside>
      </header>

      <!--
        双栏区分（色觉友好）：不靠色相，靠
        明度（浅纸 vs 深顶栏）· 形状（方 vs 圆）· 纹理（横线 vs 弧线）· 文字标签
      -->
      <div class="boards" :aria-busy="loading">
        <section class="pane pane--tasks" aria-labelledby="pane-tasks">
          <div class="pane-rules" aria-hidden="true" />
          <div class="pane-spine" aria-hidden="true" />
          <header class="pane-head">
            <div class="pane-head__left">
              <span class="pane-ico pane-ico--check" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <rect x="4" y="4" width="16" height="16" rx="2" stroke="currentColor" stroke-width="1.7" />
                  <path d="M8 12l2.8 2.8L16.5 9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <div>
                <p class="pane-tag pane-tag--square">
                  <span class="pane-tag__en">TASK</span>
                  <span class="pane-tag__zh">清单</span>
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
                  <circle cx="12" cy="12" r="8.5" stroke="currentColor" stroke-width="1.7" />
                  <path d="M12 7.5V12l3.2 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <div>
                <p class="pane-tag pane-tag--round">
                  <span class="pane-tag__en">TIME</span>
                  <span class="pane-tag__zh">时刻</span>
                </p>
                <h2 id="pane-events" class="pane-title">日程安排</h2>
              </div>
            </div>
            <button type="button" class="pane-link pane-link--on-dark" @click="goEvents">全部</button>
          </header>

          <div class="pane-body">
            <div class="pane-arcs" aria-hidden="true" />

            <div v-if="loading" class="skel skel--agenda" aria-hidden="true">
              <div v-for="n in 3" :key="`e${n}`" class="skel-row skel-row--agenda" />
            </div>

            <div v-else-if="!visibleEvents.length" class="empty empty--events">
              <div class="empty-clock" aria-hidden="true">
                <span class="empty-clock__ring" />
                <span class="empty-clock__hand empty-clock__hand--h" />
                <span class="empty-clock__hand empty-clock__hand--m" />
                <span class="empty-clock__core" />
                <span class="empty-clock__tick" style="--a: 0deg" />
                <span class="empty-clock__tick" style="--a: 90deg" />
                <span class="empty-clock__tick" style="--a: 180deg" />
                <span class="empty-clock__tick" style="--a: 270deg" />
              </div>
              <p class="empty-title">今天暂无安排</p>
              <p class="empty-desc">可以把时间留给手头的任务。</p>
              <button type="button" class="btn-ghost btn-ghost--round" @click="goEvents">去日程</button>
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
  --faint: var(--color-text-dim);
  --line: var(--color-border);
  --line-strong: var(--color-border-strong);
  --accent: var(--color-accent);
  --deep: var(--color-accent-deep);
  --danger: var(--color-red);

  position: relative;
  box-sizing: border-box;
  min-height: 100%;
  padding: 32px 36px 52px;
  font-family: var(--font-family-sans);
  color: var(--ink);
  overflow: auto;
}

/* ── Atmosphere ── */
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
  filter: blur(64px);
}

.atm-glow--a {
  width: 460px;
  height: 460px;
  top: -140px;
  right: 2%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.3), transparent 68%);
  animation: drift 14s ease-in-out infinite alternate;
}

.atm-glow--b {
  width: 340px;
  height: 340px;
  bottom: -50px;
  left: -70px;
  background: radial-gradient(circle, rgba(14, 116, 144, 0.2), transparent 70%);
  animation: drift 18s ease-in-out infinite alternate-reverse;
}

.atm-glow--c {
  width: 220px;
  height: 220px;
  top: 42%;
  left: 38%;
  background: radial-gradient(circle, rgba(8, 145, 178, 0.1), transparent 72%);
  animation: drift 22s ease-in-out infinite alternate;
}

.atm-hatch {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image: repeating-linear-gradient(
    -18deg,
    transparent,
    transparent 11px,
    rgba(8, 145, 178, 0.04) 11px,
    rgba(8, 145, 178, 0.04) 12px
  );
  mask-image: radial-gradient(ellipse 80% 70% at 85% 8%, #000 10%, transparent 68%);
}

.atm-flow {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.85;
}

.atm-flow__path {
  stroke-dasharray: 8 14;
  animation: dash 28s linear infinite;
}

.atm-flow__path--b {
  animation-duration: 36s;
  animation-direction: reverse;
}

.atm-flow__node {
  animation: pulse-node 3.6s ease-in-out infinite;
}

.atm-flow__node--b {
  animation-delay: 0.8s;
}

.atm-flow__node--c {
  animation-delay: 1.6s;
}

.atm-rings {
  position: absolute;
}

.atm-rings span {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(8, 145, 178, 0.14);
}

.atm-rings span:nth-child(2) {
  inset: 18%;
  border-style: dashed;
  opacity: 0.7;
}

.atm-rings span:nth-child(3) {
  inset: 36%;
  opacity: 0.5;
}

.atm-rings--a {
  top: -40px;
  right: 8%;
  width: 220px;
  height: 220px;
  animation: spin 80s linear infinite;
}

.atm-rings--b {
  bottom: 8%;
  left: 4%;
  width: 140px;
  height: 140px;
  animation: spin 110s linear infinite reverse;
  opacity: 0.7;
}

.atm-hex {
  position: absolute;
  top: 18%;
  left: 6%;
  width: 72px;
  height: 80px;
  opacity: 0.35;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 36' fill='none'%3E%3Cpath d='M16 2L28 9v14L16 30 4 23V9z' stroke='%230891b2' stroke-width='1.2'/%3E%3Cpath d='M16 2v28M4 9l12 7 12-7' stroke='%230891b2' stroke-width='1' opacity='.55'/%3E%3C/svg%3E")
    center / contain no-repeat;
  animation: drift 16s ease-in-out infinite alternate;
}

.atm-grain {
  position: absolute;
  inset: 0;
  opacity: 0.3;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

@keyframes drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(-18px, 14px) scale(1.06); }
}

@keyframes dash {
  to { stroke-dashoffset: -240; }
}

@keyframes pulse-node {
  0%, 100% { opacity: 0.45; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.35); }
}

.shell {
  position: relative;
  z-index: 1;
  max-width: 1120px;
  margin: 0 auto;
}

/* ── Hero ── */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(220px, 0.7fr);
  gap: 24px 40px;
  align-items: center;
  margin-bottom: 32px;
  animation: rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  color: var(--accent);
}

.hero-brand__mark {
  display: grid;
  place-items: center;
}

.hero-brand__name {
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-style: italic;
  letter-spacing: 0.01em;
}

.hero-brand__tick {
  width: 28px;
  height: 1px;
  margin-left: 4px;
  background: linear-gradient(90deg, rgba(8, 145, 178, 0.55), transparent);
}

.hero-kicker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  letter-spacing: 0.02em;
}

.hero-kicker__sep {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.35);
}

.hero-kicker__time {
  font-family: var(--font-family-mono);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1em;
  color: var(--faint);
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(2.05rem, 1.55rem + 2.1vw, 2.9rem);
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.04em;
  text-wrap: balance;
}

.hero-greet {
  font-weight: 500;
  color: var(--body);
}

.hero-name {
  background: linear-gradient(115deg, var(--deep) 12%, var(--accent) 78%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-lead {
  margin: 0;
  max-width: 34em;
  font-size: 15px;
  line-height: 1.7;
  color: var(--body);
  text-wrap: pretty;
}

/* Core meters */
.meters {
  list-style: none;
  margin: 22px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  max-width: 460px;
}

.meter {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 12px 11px;
  border: 1.5px solid var(--line-strong);
  background: #fff;
  box-shadow: inset 0 1px 0 #fff;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(0.1s + var(--i, 0) * 0.06s);
  transition: border-color 200ms ease, transform 200ms ease, box-shadow 200ms ease;
}

/* 形状区分：三角警示 / 方角清单 / 圆角时刻 —— 不依赖色相 */
.meter--overdue {
  border-radius: var(--radius-xs);
  border-style: solid;
  border-width: 1.5px 1.5px 1.5px 4px;
  border-color: var(--line-strong);
  border-left-color: var(--danger);
  background:
    repeating-linear-gradient(
      -45deg,
      #fff,
      #fff 5px,
      rgba(224, 85, 69, 0.06) 5px,
      rgba(224, 85, 69, 0.06) 10px
    );
}

.meter--due {
  border-radius: var(--radius-sm);
  border-left: 4px solid var(--accent);
  background: linear-gradient(160deg, #fff, var(--color-surface-muted));
}

.meter--events {
  border-radius: 999px 16px 16px 999px;
  border-style: dashed;
  border-color: var(--accent);
  padding-left: 14px;
  background: rgba(238, 248, 252, 0.85);
}

.meter:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-xs), inset 0 1px 0 #fff;
}

.meter--danger {
  border-left-color: var(--danger);
  background:
    repeating-linear-gradient(
      -45deg,
      var(--color-red-soft),
      var(--color-red-soft) 5px,
      rgba(224, 85, 69, 0.08) 5px,
      rgba(224, 85, 69, 0.08) 10px
    );
}

.meter--danger .meter-glyph,
.meter--danger .meter-value {
  color: var(--danger);
}

.meter--idle {
  opacity: 0.85;
}

.meter-glyph {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  margin-top: 1px;
  color: var(--accent);
  background: var(--color-accent-soft);
  border: 1.5px solid var(--line-strong);
}

.meter--overdue .meter-glyph {
  border-radius: 2px;
}

.meter--due .meter-glyph {
  border-radius: 4px;
}

.meter--events .meter-glyph {
  border-radius: 50%;
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
  letter-spacing: 0.06em;
  color: var(--muted);
}

.meter-value {
  font-family: var(--font-family-mono);
  font-size: 1.35rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.04em;
  line-height: 1.15;
  color: var(--deep);
}

.meter-hint {
  font-size: 11px;
  color: var(--faint);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
}

.btn-soft {
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--line-strong);
  border-radius: 4px 12px 12px 12px;
  background: linear-gradient(180deg, #fff, rgba(238, 248, 252, 0.9));
  color: var(--deep);
  font-size: 13px;
  font-weight: 600;
  box-shadow: var(--shadow-xs), inset 0 1px 0 #fff;
  cursor: pointer;
  transition:
    transform 200ms ease,
    box-shadow 200ms ease,
    border-color 200ms ease;
}

.btn-soft:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(8, 145, 178, 0.4);
  box-shadow: var(--shadow-sm);
}

.btn-soft:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-soft:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-text {
  border: none;
  background: none;
  padding: 4px 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--deep);
  cursor: pointer;
  text-decoration: underline;
  text-decoration-color: rgba(8, 145, 178, 0.3);
  text-underline-offset: 4px;
  transition: color 180ms ease, text-decoration-color 180ms ease;
}

.btn-text:hover {
  color: var(--accent);
  text-decoration-color: var(--accent);
}

.btn-soft:focus-visible,
.btn-text:focus-visible,
.btn-ghost:focus-visible,
.pane-link:focus-visible,
.card-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ── Day card motif ── */
.daycard {
  position: relative;
  display: flex;
  justify-content: center;
  color: var(--accent);
  animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.daycard-orbit-ring {
  position: absolute;
  width: min(100%, 280px);
  aspect-ratio: 1;
  border-radius: 50%;
  border: 1px dashed rgba(8, 145, 178, 0.18);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: spin-centered 60s linear infinite;
}

.daycard-plate {
  position: relative;
  width: min(100%, 252px);
  aspect-ratio: 11 / 12;
  padding: 18px 16px 16px;
  border-radius: 2px 28px 8px 28px;
  border: 1px solid rgba(8, 145, 178, 0.3);
  background:
    radial-gradient(ellipse 80% 55% at 50% 26%, rgba(34, 211, 238, 0.22), transparent 65%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.94), rgba(232, 246, 251, 0.78));
  box-shadow:
    var(--shadow-md),
    0 18px 40px rgba(8, 145, 178, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 0 0 1px rgba(255, 255, 255, 0.4);
  transform: rotate(2.5deg);
  transition: transform 480ms cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}

.hero:hover .daycard-plate {
  transform: rotate(0deg) translateY(-3px);
}

.daycard-plate::before {
  content: '';
  position: absolute;
  inset: 10px;
  border: 1px dashed rgba(8, 145, 178, 0.24);
  border-radius: 2px 20px 4px 20px;
  pointer-events: none;
}

.daycard-corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: rgba(8, 145, 178, 0.45);
  border-style: solid;
  z-index: 2;
}

.daycard-corner--tl {
  top: 14px;
  left: 14px;
  border-width: 1.5px 0 0 1.5px;
}

.daycard-corner--br {
  right: 14px;
  bottom: 14px;
  border-width: 0 1.5px 1.5px 0;
}

.daycard-motif {
  position: absolute;
  inset: 6% 5% auto;
  width: 90%;
  height: auto;
  opacity: 0.92;
}

.daycard-orbit {
  transform-origin: 110px 112px;
  animation: spin 40s linear infinite;
}

.daycard-orbit--rev {
  animation-direction: reverse;
  animation-duration: 55s;
}

.daycard-num {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 70%;
  padding-bottom: 6px;
  pointer-events: none;
}

.daycard-month {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--deep);
  opacity: 0.78;
}

.daycard-day {
  font-family: var(--font-family-mono);
  font-size: clamp(58px, 8vw, 76px);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.06em;
  line-height: 0.88;
  background: linear-gradient(180deg, var(--ink) 18%, var(--deep));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 10px 18px rgba(8, 145, 178, 0.14));
}

.daycard-marks {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: auto;
}

.daycard-mark {
  position: relative;
  display: grid;
  place-items: center;
  width: 34px;
  height: 30px;
  border-radius: 2px 8px 8px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--faint);
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid var(--line);
  transition: color 200ms ease, background 200ms ease, border-color 200ms ease, box-shadow 200ms ease;
}

.daycard-mark.is-lit {
  color: var(--deep);
  background: rgba(8, 145, 178, 0.14);
  border-color: rgba(8, 145, 178, 0.36);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.08);
}

.daycard-mark__dot {
  position: absolute;
  top: 4px;
  right: 5px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.25);
}

/* ── Boards：色觉友好 — 明度 / 形状 / 纹理 / 标签 ── */
.boards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: stretch;
  animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.12s both;
}

.pane {
  position: relative;
  z-index: 1;
  min-height: 380px;
  overflow: hidden;
}

/* 左：浅纸 + 横线纹理 + 方角 + 粗竖脊 —— 灰度也能认 */
.pane--tasks {
  padding: 24px 22px 28px 28px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--line-strong);
  border-left: none;
  background: var(--color-surface);
  box-shadow: var(--shadow-sm), 3px 3px 0 rgba(8, 145, 178, 0.12);
}

.pane-spine {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 8px;
  background: linear-gradient(180deg, var(--accent), var(--deep));
}

.pane-rules {
  pointer-events: none;
  position: absolute;
  inset: 0 0 0 8px;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent 27px,
    rgba(8, 145, 178, 0.1) 27px,
    rgba(8, 145, 178, 0.1) 28px
  );
  mask-image: linear-gradient(180deg, transparent 0, #000 72px, #000 100%);
}

/* 右：深色顶栏 + 圆角体 + 弧线纹理 */
.pane--events {
  display: flex;
  flex-direction: column;
  padding: 0;
  border-radius: var(--radius-2xl);
  border: 1.5px solid var(--line-strong);
  background: var(--color-surface);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.pane-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px 16px;
  background:
    linear-gradient(118deg, var(--color-zone-topbar-from), var(--color-zone-topbar-mid) 55%, var(--color-zone-topbar-to));
  color: var(--color-zone-topbar-text);
  box-shadow: inset 0 -1px 0 var(--color-zone-topbar-border);
}

.pane-banner__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.pane-body {
  position: relative;
  flex: 1;
  padding: 20px 22px 26px;
  min-height: 0;
}

.pane-arcs {
  pointer-events: none;
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  border: 2px solid rgba(8, 145, 178, 0.18);
  box-shadow:
    inset 0 0 0 18px transparent,
    0 0 0 14px rgba(8, 145, 178, 0.06);
}

.pane-arcs::before,
.pane-arcs::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(8, 145, 178, 0.14);
}

.pane-arcs::before {
  inset: 22%;
}

.pane-arcs::after {
  inset: 42%;
  border-style: dashed;
}

.pane--tasks > :not(.pane-rules):not(.pane-spine) {
  position: relative;
  z-index: 1;
}

.pane-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.pane-head__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pane-tag {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 4px;
  line-height: 1;
}

.pane-tag__en {
  font-family: var(--font-family-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.pane-tag__zh {
  font-size: 12px;
  font-weight: 600;
}

.pane-tag--square {
  padding: 3px 8px 3px 6px;
  border-radius: var(--radius-xs);
  border: 1.5px solid var(--deep);
  background: var(--color-accent-soft);
  color: var(--deep);
}

.pane-tag--round {
  padding: 3px 10px;
  border-radius: 999px;
  border: 1.5px solid rgba(240, 249, 255, 0.4);
  background: rgba(255, 255, 255, 0.12);
  color: var(--color-zone-topbar-text);
}

.pane-ico {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.pane-ico--check {
  border-radius: var(--radius-sm);
  color: #fff;
  background: var(--deep);
  border: 2px solid var(--deep);
}

.pane-ico--clock {
  border-radius: 50%;
  color: var(--deep);
  background: #fff;
  border: 2px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.35);
}

.pane-ico svg {
  width: 20px;
  height: 20px;
}

.pane-title {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.pane-banner .pane-title {
  color: var(--color-zone-topbar-text);
}

.pane-link {
  border: none;
  background: none;
  padding: 6px 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: solid;
  text-underline-offset: 3px;
  transition: color 180ms ease;
}

.pane-link:hover {
  color: var(--accent);
}

.pane-link--on-dark {
  color: var(--color-zone-topbar-text-dim);
  text-decoration-color: rgba(240, 249, 255, 0.35);
}

.pane-link--on-dark:hover {
  color: #fff;
}

.btn-ghost {
  height: 34px;
  padding: 0 14px;
  border: 1.5px solid var(--line-strong);
  border-radius: var(--radius-xs) var(--radius-md) var(--radius-md) var(--radius-md);
  background: linear-gradient(180deg, #fff, var(--color-surface-muted));
  color: var(--deep);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.btn-ghost:hover {
  border-color: rgba(8, 145, 178, 0.4);
  box-shadow: var(--shadow-xs);
  transform: translateY(-1px);
}

.btn-ghost:active {
  transform: scale(0.98);
}

.btn-ghost--round {
  border-radius: 999px;
}

/* ── Cards ── */
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list--tasks {
  gap: 6px;
}

.list--agenda {
  position: relative;
  gap: 0;
  padding-left: 0;
}

.card {
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(0.14s + var(--i, 0) * 0.05s);
}

.card-btn {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 1px solid transparent;
  text-align: left;
  cursor: pointer;
  transition:
    transform 200ms ease,
    background 200ms ease,
    border-color 200ms ease,
    box-shadow 200ms ease;
}

.card--task .card-btn {
  border-radius: 2px;
  background: #fff;
  border: 1.5px solid rgba(12, 74, 94, 0.22);
}

.card--task .card-btn:hover {
  background: #fff;
  border-color: var(--deep);
  box-shadow: 2px 2px 0 rgba(6, 36, 64, 0.08);
  transform: translateX(2px);
}

.card-btn:active {
  transform: scale(0.99);
}

.card.is-today .card-btn {
  background: var(--color-accent-soft);
  border-color: var(--line-strong);
  border-width: 2px;
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
  width: 18px;
  height: 18px;
  margin-top: 2px;
  border-radius: var(--radius-xs);
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
  padding: 10px 12px 10px 0;
  border-radius: 0;
  background: transparent;
  border: none;
}

.card-btn--event:hover {
  background: rgba(255, 255, 255, 0.55);
  transform: none;
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
  background: linear-gradient(180deg, var(--accent), rgba(8, 145, 178, 0.15));
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
  width: 3.4rem;
  padding-top: 4px;
  font-family: var(--font-family-mono);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
  color: var(--deep);
  text-align: right;
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding-top: 2px;
}

.card-title {
  font-size: 14px;
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
  font-size: 12px;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

.card-badge {
  display: inline-flex;
  padding: 1px 8px;
  border-radius: var(--radius-xs) 7px 7px 7px;
  font-weight: 700;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--deep);
  background: var(--color-accent-soft);
  border: 1.5px solid var(--line-strong);
}

.card-badge--warn,
.card.is-overdue .card-badge {
  color: #9a3d32;
  border-color: rgba(224, 85, 69, 0.35);
  background:
    repeating-linear-gradient(
      -45deg,
      var(--color-red-soft),
      var(--color-red-soft) 3px,
      #fff 3px,
      #fff 6px
    );
}

.card.is-today .card-badge {
  background: var(--deep);
  color: #fff;
  border-color: var(--deep);
}

/* Empty states — 清单行 vs 表盘 */
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
  padding-top: 36px;
}

.empty-stack {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(100%, 200px);
  margin-bottom: 16px;
  padding: 14px;
  border-radius: var(--radius-sm);
  border: 1.5px dashed var(--deep);
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
  border-radius: var(--radius-xs);
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
  background: rgba(8, 145, 178, 0.18);
}

.empty-line--mid { width: 70%; flex: 0 1 70%; }
.empty-line--short { width: 48%; flex: 0 1 48%; }

.empty-clock {
  position: relative;
  z-index: 1;
  width: 96px;
  height: 96px;
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

.empty-clock__tick {
  position: absolute;
  left: 50%;
  top: 8px;
  width: 2px;
  height: 10px;
  margin-left: -1px;
  background: var(--deep);
  transform: rotate(var(--a));
  transform-origin: 50% 40px;
}

.empty-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 0 0 12px;
  max-width: 22em;
  font-size: 13px;
  line-height: 1.55;
  color: var(--muted);
}

.skel--agenda .skel-row--agenda {
  border-radius: 999px 14px 14px 999px;
}

.more {
  margin: 14px 2px 0;
  font-size: 12px;
  color: var(--muted);
}

.skel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skel-row {
  height: 56px;
  border-radius: 4px 14px 12px 12px;
  background: linear-gradient(
    90deg,
    rgba(8, 145, 178, 0.05),
    rgba(8, 145, 178, 0.12),
    rgba(8, 145, 178, 0.05)
  );
  background-size: 200% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes spin-centered {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

@media (max-width: 900px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .daycard {
    justify-content: flex-start;
  }

  .daycard-plate {
    width: 190px;
    transform: none;
  }

  .daycard-orbit-ring {
    display: none;
  }

  .boards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .home {
    padding: 20px 14px 36px;
  }

  .meters {
    grid-template-columns: 1fr;
    max-width: none;
  }

  .pane {
    padding: 20px 16px 22px;
    min-height: 0;
  }

  .hero-title {
    font-size: 1.75rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero,
  .daycard,
  .boards,
  .card,
  .meter,
  .atm-glow,
  .atm-rings,
  .atm-hex,
  .atm-flow__path,
  .atm-flow__node,
  .daycard-orbit,
  .daycard-orbit-ring,
  .skel-row,
  .btn-soft,
  .card-btn,
  .daycard-plate {
    animation: none !important;
    transition: none !important;
  }
}
</style>
