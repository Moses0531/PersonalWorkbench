<script setup>
/**
 * 工作台首页
 * 构图：问候 + 日签图案锚点；内容：三天内截止 / 今日日程。
 * 图案服务于氛围，不抢数据。
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
    <!-- 氛围层：光晕 + 细纹 + 颗粒 -->
    <div class="atm" aria-hidden="true">
      <div class="atm-glow atm-glow--a" />
      <div class="atm-glow atm-glow--b" />
      <div class="atm-hatch" />
      <div class="atm-grain" />
    </div>

    <div class="shell">
      <!-- 主视觉：文案 + 日签图案 -->
      <header class="hero">
        <div class="hero-copy">
          <div class="hero-brand">
            <span class="hero-brand__mark"><BrandMarkView :size="20" /></span>
            <span class="hero-brand__name">Workbench</span>
          </div>

          <p class="hero-kicker">{{ dateLabel }}<span>{{ timeLabel }}</span></p>
          <h1 class="hero-title">
            <span class="hero-greet">{{ greeting }}，</span>
            <span class="hero-name">{{ displayName }}</span>
          </h1>
          <p class="hero-lead">{{ focusSummary }}</p>

          <div class="hero-actions">
            <button type="button" class="btn-soft" :disabled="loading" @click="refreshFocus">
              {{ loading ? '整理中…' : '刷新重点' }}
            </button>
            <button type="button" class="btn-text" @click="goTasks">打开事务</button>
          </div>
        </div>

        <!-- 日签：首页主图案 -->
        <aside class="daycard" aria-hidden="true">
          <div class="daycard-plate">
            <svg class="daycard-motif" viewBox="0 0 220 240" fill="none">
              <defs>
                <linearGradient id="dcFill" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="rgba(34,211,238,0.35)" />
                  <stop offset="100%" stop-color="rgba(8,145,178,0.08)" />
                </linearGradient>
                <linearGradient id="dcRing" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(8,145,178,0.45)" />
                  <stop offset="100%" stop-color="rgba(8,145,178,0.05)" />
                </linearGradient>
              </defs>
              <!-- 底纹圆 -->
              <circle cx="110" cy="118" r="96" stroke="url(#dcRing)" stroke-width="1" opacity="0.7" />
              <circle class="daycard-orbit" cx="110" cy="118" r="78" stroke="currentColor" stroke-width="0.8" stroke-dasharray="3 7" opacity="0.28" />
              <!-- 机匣 -->
              <path
                d="M110 52 L148 72 V124 L110 144 L72 124 V72 Z"
                fill="url(#dcFill)"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linejoin="round"
              />
              <path
                d="M110 52 V144 M72 72 L148 72 M72 124 L148 124 M72 72 L110 98 L148 72 M72 124 L110 98 L148 124"
                stroke="currentColor"
                stroke-width="1.15"
                stroke-linejoin="round"
                opacity="0.55"
              />
              <circle cx="110" cy="98" r="3.5" fill="currentColor" opacity="0.7" />
              <!-- 勾选 / 时钟小印 -->
              <g opacity="0.5" transform="translate(28 168)">
                <rect width="16" height="16" rx="3" stroke="currentColor" stroke-width="1.3" />
                <path d="M4 8.2l2.8 2.8L12.2 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" fill="none" />
              </g>
              <g opacity="0.5" transform="translate(176 166)">
                <circle cx="8" cy="8" r="8" stroke="currentColor" stroke-width="1.3" />
                <path d="M8 4.2V8.2l3 2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" fill="none" />
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
              >
                {{ m.label }}
              </span>
            </div>
          </div>
        </aside>
      </header>

      <!-- 内容板 -->
      <div class="board" :aria-busy="loading">
        <div class="board-sheen" aria-hidden="true" />

        <section class="pane" aria-labelledby="pane-tasks">
          <header class="pane-head">
            <div class="pane-head__left">
              <span class="pane-ico" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <rect x="4" y="3.5" width="16" height="17" rx="3" stroke="currentColor" stroke-width="1.5" />
                  <path d="M8 9h8M8 13h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  <path d="M8 17h3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.5" />
                </svg>
              </span>
              <div>
                <p class="pane-eyebrow">优先</p>
                <h2 id="pane-tasks" class="pane-title">三天内截止</h2>
              </div>
            </div>
            <button type="button" class="pane-link" @click="goTasks">全部</button>
          </header>

          <div v-if="loading" class="skel" aria-hidden="true">
            <div v-for="n in 3" :key="n" class="skel-row" />
          </div>

          <div v-else-if="!visibleTasks.length" class="empty">
            <svg class="empty-art" viewBox="0 0 96 56" fill="none" aria-hidden="true">
              <path d="M12 40c10-16 24-22 36-22s26 6 36 22" stroke="currentColor" stroke-width="1.2" opacity="0.35" />
              <rect x="34" y="16" width="28" height="24" rx="4" stroke="currentColor" stroke-width="1.4" opacity="0.5" />
              <path d="M42 28l4 4 8-9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" fill="none" opacity="0.7" />
            </svg>
            <p class="empty-title">没有临近截止</p>
            <p class="empty-desc">近三天没有未完成的到期任务。</p>
            <button type="button" class="btn-text" @click="goTasks">去事务</button>
          </div>

          <ul v-else class="list" role="list">
            <li
              v-for="(task, idx) in visibleTasks"
              :key="task.taskId"
              class="card"
              :class="{ 'is-overdue': task.overdue, 'is-today': !task.overdue && task.daysLeft === 0 }"
              :style="{ '--i': idx }"
            >
              <button type="button" class="card-btn" @click="goTasks">
                <span class="card-accent" aria-hidden="true" />
                <span class="card-main">
                  <span class="card-title">{{ task.title || '未命名任务' }}</span>
                  <span class="card-meta">
                    <span class="card-badge">{{ dueLabel(task) }}</span>
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
          <header class="pane-head">
            <div class="pane-head__left">
              <span class="pane-ico" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.5" />
                  <path d="M12 8v4.2l3 1.8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
              </span>
              <div>
                <p class="pane-eyebrow">今天</p>
                <h2 id="pane-events" class="pane-title">日程安排</h2>
              </div>
            </div>
            <button type="button" class="pane-link" @click="goEvents">全部</button>
          </header>

          <div v-if="loading" class="skel" aria-hidden="true">
            <div v-for="n in 3" :key="`e${n}`" class="skel-row" />
          </div>

          <div v-else-if="!visibleEvents.length" class="empty">
            <svg class="empty-art" viewBox="0 0 96 56" fill="none" aria-hidden="true">
              <circle cx="48" cy="28" r="18" stroke="currentColor" stroke-width="1.2" opacity="0.35" />
              <path d="M48 16v12l8 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity="0.65" />
            </svg>
            <p class="empty-title">今天暂无安排</p>
            <p class="empty-desc">可以把时间留给手头的任务。</p>
            <button type="button" class="btn-text" @click="goEvents">去日程</button>
          </div>

          <ul v-else class="list list--agenda" role="list">
            <li
              v-for="(ev, idx) in visibleEvents"
              :key="ev.eventId"
              class="card card--event"
              :style="{ '--i': idx }"
            >
              <button type="button" class="card-btn" @click="goEvents">
                <span class="card-dot" aria-hidden="true" />
                <time class="card-clock">{{ formatEventTime(ev) }}</time>
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
  padding: 32px 36px 48px;
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
  width: 420px;
  height: 420px;
  top: -120px;
  right: 5%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.28), transparent 68%);
  animation: drift 14s ease-in-out infinite alternate;
}

.atm-glow--b {
  width: 320px;
  height: 320px;
  bottom: -40px;
  left: -60px;
  background: radial-gradient(circle, rgba(14, 116, 144, 0.18), transparent 70%);
  animation: drift 18s ease-in-out infinite alternate-reverse;
}

.atm-hatch {
  position: absolute;
  inset: 0;
  opacity: 0.4;
  background-image: repeating-linear-gradient(
    -18deg,
    transparent,
    transparent 11px,
    rgba(8, 145, 178, 0.035) 11px,
    rgba(8, 145, 178, 0.035) 12px
  );
  mask-image: radial-gradient(ellipse 80% 70% at 80% 10%, #000 10%, transparent 70%);
}

.atm-grain {
  position: absolute;
  inset: 0;
  opacity: 0.28;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

@keyframes drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(-18px, 14px) scale(1.06); }
}

.shell {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
}

/* ── Hero ── */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(200px, 0.72fr);
  gap: 20px 36px;
  align-items: center;
  margin-bottom: 28px;
  animation: rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--accent);
}

.hero-brand__mark {
  display: grid;
  place-items: center;
}

.hero-brand__name {
  font-family: var(--font-family-serif);
  font-size: 14px;
  font-style: italic;
  letter-spacing: 0.02em;
}

.hero-kicker {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  letter-spacing: 0.02em;
}

.hero-kicker span {
  margin-left: 12px;
  font-family: var(--font-family-mono);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.08em;
  color: var(--faint);
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(2rem, 1.5rem + 2vw, 2.75rem);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.038em;
  text-wrap: balance;
}

.hero-greet {
  font-weight: 500;
  color: var(--body);
}

.hero-name {
  background: linear-gradient(115deg, var(--deep) 15%, var(--accent) 80%);
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
.pane-link:focus-visible,
.card-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ── Day card motif ── */
.daycard {
  display: flex;
  justify-content: center;
  color: var(--accent);
  animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.daycard-plate {
  position: relative;
  width: min(100%, 240px);
  aspect-ratio: 11 / 12;
  padding: 18px 16px 16px;
  border-radius: 2px 28px 8px 28px;
  border: 1px solid rgba(8, 145, 178, 0.28);
  background:
    radial-gradient(ellipse 80% 60% at 50% 28%, rgba(34, 211, 238, 0.18), transparent 65%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.92), rgba(232, 246, 251, 0.75));
  box-shadow:
    var(--shadow-md),
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 0 0 1px rgba(255, 255, 255, 0.4);
  transform: rotate(2deg);
  transition: transform 480ms cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}

.hero:hover .daycard-plate {
  transform: rotate(0deg) translateY(-2px);
}

.daycard-plate::before {
  content: '';
  position: absolute;
  inset: 10px;
  border: 1px dashed rgba(8, 145, 178, 0.22);
  border-radius: 2px 20px 4px 20px;
  pointer-events: none;
}

.daycard-motif {
  position: absolute;
  inset: 8% 6% auto;
  width: 88%;
  height: auto;
  opacity: 0.9;
}

.daycard-orbit {
  transform-origin: 110px 118px;
  animation: spin 40s linear infinite;
}

.daycard-num {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 72%;
  padding-bottom: 8px;
  pointer-events: none;
}

.daycard-month {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--deep);
  opacity: 0.75;
}

.daycard-day {
  font-family: var(--font-family-mono);
  font-size: clamp(56px, 8vw, 72px);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.06em;
  line-height: 0.9;
  background: linear-gradient(180deg, var(--ink) 20%, var(--deep));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: none;
  filter: drop-shadow(0 8px 16px rgba(8, 145, 178, 0.12));
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
  display: grid;
  place-items: center;
  width: 32px;
  height: 28px;
  border-radius: 2px 8px 8px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--faint);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--line);
  transition: color 200ms ease, background 200ms ease, border-color 200ms ease;
}

.daycard-mark.is-lit {
  color: var(--deep);
  background: rgba(8, 145, 178, 0.12);
  border-color: rgba(8, 145, 178, 0.32);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.08);
}

/* ── Board ── */
.board {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  border-radius: 4px 22px 20px 20px;
  border: 1px solid var(--line-strong);
  background: rgba(255, 255, 255, 0.7);
  box-shadow:
    var(--shadow-md),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  overflow: hidden;
  animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) 0.12s both;
}

.board-sheen {
  pointer-events: none;
  position: absolute;
  inset: 0;
  background:
    linear-gradient(125deg, rgba(255, 255, 255, 0.55) 0%, transparent 42%),
    radial-gradient(ellipse 50% 40% at 100% 0%, rgba(34, 211, 238, 0.08), transparent 60%);
  z-index: 0;
}

.pane {
  position: relative;
  z-index: 1;
  padding: 26px 26px 28px;
  min-height: 340px;
}

.pane--events {
  border-left: 1px solid var(--color-divider);
  background: linear-gradient(180deg, rgba(238, 248, 252, 0.35), transparent 40%);
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

.pane-ico {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 10px 16px 12px 12px;
  color: var(--accent);
  background:
    linear-gradient(145deg, rgba(8, 145, 178, 0.16), rgba(255, 255, 255, 0.6));
  border: 1px solid rgba(8, 145, 178, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 4px 12px rgba(8, 145, 178, 0.08);
}

.pane-ico svg {
  width: 20px;
  height: 20px;
}

.pane-eyebrow {
  margin: 0 0 2px;
  font-family: var(--font-family-serif);
  font-size: 12px;
  font-style: italic;
  color: var(--accent);
}

.pane-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.pane-link {
  border: none;
  background: none;
  padding: 6px 2px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition: color 180ms ease;
}

.pane-link:hover {
  color: var(--accent);
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

.list--agenda {
  position: relative;
  padding-left: 2px;
}

.list--agenda::before {
  content: '';
  position: absolute;
  left: 17px;
  top: 22px;
  bottom: 22px;
  width: 2px;
  border-radius: 1px;
  background: linear-gradient(180deg, rgba(8, 145, 178, 0.4), rgba(8, 145, 178, 0.05));
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
  padding: 12px 12px;
  border: 1px solid transparent;
  border-radius: 4px 14px 12px 12px;
  background: rgba(255, 255, 255, 0.55);
  text-align: left;
  cursor: pointer;
  transition:
    transform 200ms ease,
    background 200ms ease,
    border-color 200ms ease,
    box-shadow 200ms ease;
}

.card-btn:hover {
  background: #fff;
  border-color: var(--line);
  box-shadow: var(--shadow-xs);
  transform: translateY(-2px);
}

.card-btn:active {
  transform: scale(0.99);
}

.card.is-today .card-btn {
  background: rgba(8, 145, 178, 0.08);
  border-color: rgba(8, 145, 178, 0.18);
}

.card.is-overdue .card-btn {
  background: rgba(224, 85, 69, 0.05);
  border-color: rgba(224, 85, 69, 0.14);
}

.card-accent {
  flex-shrink: 0;
  width: 3px;
  align-self: stretch;
  min-height: 34px;
  margin-top: 2px;
  border-radius: 2px;
  background: rgba(8, 145, 178, 0.25);
}

.card.is-today .card-accent {
  background: linear-gradient(180deg, var(--accent), var(--deep));
  box-shadow: 0 0 8px rgba(8, 145, 178, 0.35);
}

.card.is-overdue .card-accent {
  background: var(--danger);
  box-shadow: 0 0 8px rgba(224, 85, 69, 0.25);
}

.card-dot {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  width: 10px;
  height: 10px;
  margin: 5px 4px 0;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 4px rgba(8, 145, 178, 0.14);
}

.card-clock {
  flex-shrink: 0;
  width: 3.1rem;
  padding-top: 1px;
  font-family: var(--font-family-mono);
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--deep);
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
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
  border-radius: 2px 7px 7px 7px;
  font-weight: 600;
  color: var(--deep);
  background: rgba(8, 145, 178, 0.1);
  border: 1px solid rgba(8, 145, 178, 0.18);
}

.card.is-overdue .card-badge {
  color: #9a3d32;
  background: var(--color-red-soft);
  border-color: rgba(224, 85, 69, 0.22);
}

.card.is-today .card-badge {
  background: rgba(8, 145, 178, 0.14);
}

/* empty / skel / more */
.empty {
  padding: 20px 4px 8px;
}

.empty-art {
  width: 96px;
  margin-bottom: 10px;
  color: var(--accent);
}

.empty-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 0 0 12px;
  max-width: 28em;
  font-size: 13px;
  line-height: 1.55;
  color: var(--muted);
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
    rgba(8, 145, 178, 0.11),
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
    width: 180px;
    transform: none;
  }

  .board {
    grid-template-columns: 1fr;
  }

  .pane--events {
    border-left: none;
    border-top: 1px solid var(--color-divider);
  }
}

@media (max-width: 520px) {
  .home {
    padding: 20px 14px 32px;
  }

  .pane {
    padding: 20px 16px 22px;
    min-height: 0;
  }

  .hero-title {
    font-size: 1.7rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero,
  .daycard,
  .board,
  .card,
  .atm-glow,
  .daycard-orbit,
  .skel-row,
  .btn-soft,
  .card-btn,
  .daycard-plate {
    animation: none;
    transition: none;
  }
}
</style>
