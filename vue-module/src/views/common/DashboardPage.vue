<script setup>
/** 登录后首页：放大欢迎主面 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BrandMarkView from '@/components/BrandMarkView.vue'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()
const now = ref(new Date())
let clockTimer = null

onMounted(() => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 30000)
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
  const d = now.value
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day} · 周${weekdays[d.getDay()]}`
})

const timeLabel = computed(() => {
  const d = now.value
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
})

function goProfile() {
  router.push('/profile').catch(() => {})
}

function openCmdHint() {
  window.dispatchEvent(
    new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }),
  )
}
</script>

<template>
  <div class="home">
    <div class="home-grain" aria-hidden="true" />

    <section class="hero panel" aria-labelledby="home-greeting">
      <div class="hero-meta">
        <span class="meta-chip">{{ dateLabel }}</span>
        <span class="meta-time" aria-hidden="true">{{ timeLabel }}</span>
      </div>

      <div class="hero-body">
        <p class="hero-kicker">Spine Extension</p>
        <h1 id="home-greeting" class="hero-title">
          <span class="hero-greet">{{ greeting }}，</span>
          <span class="hero-name">{{ displayName }}</span>
        </h1>
        <p class="hero-lead">
          这是系统入口，不是数据墙。从左侧轨道进入你负责的能力域；需要时用命令面板直达。
        </p>

        <div class="hero-actions">
          <button type="button" class="btn-primary" @click="openCmdHint">
            打开命令面板
            <kbd>Ctrl K</kbd>
          </button>
          <button type="button" class="btn-text" @click="goProfile">个人中心</button>
        </div>
      </div>

      <aside class="hero-figure" aria-hidden="true">
        <div class="spine-board">
          <svg class="spine-svg" viewBox="0 0 200 220" fill="none">
            <defs>
              <linearGradient id="spineFill" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="rgba(34,211,238,0.35)" />
                <stop offset="100%" stop-color="rgba(8,145,178,0.12)" />
              </linearGradient>
            </defs>
            <g class="grid-lines" stroke="currentColor" stroke-width="0.6" opacity="0.18">
              <line x1="20" y1="20" x2="180" y2="20" />
              <line x1="20" y1="60" x2="180" y2="60" />
              <line x1="20" y1="100" x2="180" y2="100" />
              <line x1="20" y1="140" x2="180" y2="140" />
              <line x1="20" y1="180" x2="180" y2="180" />
              <line x1="40" y1="10" x2="40" y2="200" />
              <line x1="100" y1="10" x2="100" y2="200" />
              <line x1="160" y1="10" x2="160" y2="200" />
            </g>
            <path
              class="spine-core"
              d="M100 28 L148 52 V148 L100 172 L52 148 V52 Z"
              fill="url(#spineFill)"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linejoin="round"
            />
            <path
              d="M100 28 V172 M52 52 L148 52 M52 148 L148 148 M52 52 L100 100 L148 52 M52 148 L100 100 L148 148"
              stroke="currentColor"
              stroke-width="1.2"
              stroke-linejoin="round"
              opacity="0.55"
            />
            <circle cx="100" cy="100" r="4" fill="currentColor" opacity="0.7" />
            <circle class="orbit" cx="100" cy="100" r="38" stroke="currentColor" stroke-width="0.8" stroke-dasharray="3 5" opacity="0.35" />
          </svg>
          <div class="spine-caption">
            <BrandMarkView :size="20" />
            <span>structure · entry</span>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.home {
  --home-ink: var(--color-text-primary);
  --home-body: var(--color-text-body);
  --home-dim: var(--color-text-secondary);
  --home-line: var(--color-border-strong);
  --home-accent: var(--color-accent);
  --home-deep: var(--color-accent-deep);
  --home-surface: rgba(255, 255, 255, 0.78);
  --home-inset: rgba(8, 145, 178, 0.06);

  position: relative;
  box-sizing: border-box;
  min-height: 100%;
  height: 100%;
  padding: 28px 32px 40px;
  font-family: var(--font-family-sans);
  color: var(--home-ink);
  overflow: hidden;
  display: flex;
  background: transparent;
}

.home-grain {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.35;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  mix-blend-mode: soft-light;
}

.panel {
  background: var(--home-surface);
  border: 1px solid var(--home-line);
  border-radius: 4px 20px 20px 20px;
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(10px);
  animation: rise 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
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

.hero {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(240px, 0.85fr);
  grid-template-rows: auto 1fr;
  gap: 12px 40px;
  padding: 36px 40px 40px;
  overflow: hidden;
  min-height: min(720px, calc(100dvh - 120px));
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: linear-gradient(180deg, var(--home-accent), transparent 85%);
}

.hero-meta {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: var(--home-deep);
  background: var(--home-inset);
  border: 1px solid var(--color-border);
  border-radius: 2px 8px 8px 8px;
}

.meta-time {
  font-family: var(--font-family-mono);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  color: var(--home-dim);
  letter-spacing: 0.08em;
}

.hero-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 40rem;
  padding: 12px 0 8px;
}

.hero-kicker {
  margin: 0 0 14px;
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-style: italic;
  color: var(--home-accent);
  letter-spacing: 0.02em;
}

.hero-title {
  margin: 0 0 18px;
  font-size: clamp(36px, 5vw, 56px);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.035em;
  text-wrap: balance;
  color: var(--home-ink);
}

.hero-greet {
  font-weight: 500;
  color: var(--home-body);
}

.hero-name {
  background: linear-gradient(120deg, var(--home-deep), var(--home-accent) 60%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-lead {
  margin: 0 0 28px;
  max-width: 36em;
  font-size: 15px;
  line-height: 1.75;
  color: var(--home-body);
  text-wrap: pretty;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 20px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 18px;
  border: none;
  border-radius: 4px 12px 12px 12px;
  font-size: 14px;
  font-weight: 600;
  color: #f0f9ff;
  background: linear-gradient(135deg, #22d3ee, #0891b2 55%, #0e7490);
  box-shadow: var(--shadow-accent);
  cursor: pointer;
  transition:
    transform 220ms ease,
    box-shadow 220ms ease,
    filter 220ms ease;
}

.btn-primary:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
  box-shadow: 0 6px 20px rgba(8, 145, 178, 0.36);
}

.btn-primary:active {
  transform: scale(0.98);
}

.btn-primary:focus-visible {
  outline: 2px solid var(--home-accent);
  outline-offset: 3px;
}

.btn-primary kbd {
  padding: 2px 7px;
  border-radius: 4px;
  font-family: var(--font-family-mono);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: rgba(240, 249, 255, 0.92);
  background: rgba(6, 36, 64, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-text {
  border: none;
  background: transparent;
  padding: 6px 2px;
  font-size: 14px;
  font-weight: 500;
  color: var(--home-deep);
  cursor: pointer;
  text-decoration: underline;
  text-decoration-color: rgba(8, 145, 178, 0.35);
  text-underline-offset: 4px;
  transition: color 200ms ease, text-decoration-color 200ms ease;
}

.btn-text:hover {
  color: var(--home-accent);
  text-decoration-color: var(--home-accent);
}

.btn-text:focus-visible {
  outline: 2px solid var(--home-accent);
  outline-offset: 3px;
  border-radius: 4px;
}

.hero-figure {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--home-accent);
  padding: 8px 0;
}

.spine-board {
  width: 100%;
  max-width: 320px;
  padding: 20px 18px 18px;
  border: 1px dashed rgba(8, 145, 178, 0.35);
  border-radius: 2px 20px 2px 20px;
  background:
    radial-gradient(ellipse 80% 60% at 50% 30%, rgba(34, 211, 238, 0.12), transparent 70%),
    rgba(238, 248, 252, 0.65);
  transform: rotate(1.5deg);
  transition: transform 320ms ease;
}

.hero:hover .spine-board {
  transform: rotate(0deg);
}

.spine-svg {
  display: block;
  width: 100%;
  height: auto;
}

.orbit {
  transform-origin: 100px 100px;
  animation: spin 28s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spine-caption {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
  font-family: var(--font-family-mono);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: lowercase;
  color: var(--home-dim);
}

@media (max-width: 860px) {
  .home {
    padding: 20px 16px 28px;
    height: auto;
  }

  .hero {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    gap: 20px;
    padding: 28px 24px 32px;
    min-height: 0;
  }

  .hero-figure {
    order: -1;
    justify-content: flex-start;
  }

  .spine-board {
    max-width: 200px;
    transform: none;
  }

  .hero-body {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .hero-title {
    font-size: 30px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .panel,
  .orbit,
  .btn-primary {
    animation: none;
    transition: none;
  }
}
</style>
