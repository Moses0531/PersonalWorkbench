<template>
  <div class="not-found" ref="sceneRef" @mousemove="onMouseMove">
    <!-- 纸张纹理叠加 -->
    <div class="texture"></div>

    <!-- 背景色块 -->
    <div class="bg-blob bg-blob--1"></div>
    <div class="bg-blob bg-blob--2"></div>
    <div class="bg-blob bg-blob--3"></div>

    <!-- 浮动粒子 -->
    <div class="particles">
      <span v-for="n in 12" :key="n" class="particle" :class="`particle--${n}`"></span>
    </div>

    <!-- 光线装饰 -->
    <div class="rays" :style="rayStyle">
      <div class="ray ray--1"></div>
      <div class="ray ray--2"></div>
      <div class="ray ray--3"></div>
    </div>

    <div class="content">
      <!-- 插画区 -->
      <div class="illustration" :style="illustrationStyle">
        <svg viewBox="0 0 320 240" class="scene">
          <!-- 地面光晕 -->
          <ellipse cx="160" cy="210" rx="120" ry="12" fill="rgba(224,122,79,0.06)" />
          <ellipse cx="160" cy="210" rx="80" ry="6" fill="rgba(224,122,79,0.04)" />

          <!-- 远景小星球 -->
          <g opacity="0.2" class="far-planet">
            <circle cx="50" cy="50" r="8" fill="var(--accent)" />
            <circle cx="50" cy="50" r="3" fill="rgba(255,255,255,0.4)" />
          </g>

          <!-- 主体星球 -->
          <g class="planet-group">
            <!-- 发光底色 -->
            <circle cx="160" cy="110" r="58" fill="rgba(224,122,79,0.04)" class="planet-glow" />

            <!-- 轨道环 1 -->
            <ellipse cx="160" cy="112" rx="100" ry="24" fill="none"
                     stroke="var(--accent)" stroke-width="1" stroke-dasharray="4 8"
                     opacity="0.2" class="orbit-ring orbit-ring--1" />
            <!-- 轨道环 2 -->
            <ellipse cx="160" cy="112" rx="78" ry="18" fill="none"
                     stroke="var(--accent)" stroke-width="0.8" stroke-dasharray="2 6"
                     opacity="0.12" class="orbit-ring orbit-ring--2" />

            <!-- 星球主体 -->
            <circle cx="160" cy="108" r="48" fill="var(--planet-fill)" />
            <circle cx="160" cy="108" r="48" fill="url(#planetGrad)" />

            <!-- 星球表面纹理 -->
            <ellipse cx="145" cy="96" rx="12" ry="7" fill="rgba(255,255,255,0.3)" />
            <ellipse cx="175" cy="115" rx="7" ry="5" fill="rgba(255,255,255,0.18)" />
            <ellipse cx="152" cy="122" rx="16" ry="5" fill="rgba(255,255,255,0.12)" />
            <ellipse cx="168" cy="94" rx="5" ry="4" fill="rgba(0,0,0,0.03)" />

            <!-- 小卫星 1 -->
            <circle cx="242" cy="82" r="9" fill="var(--accent)" opacity="0.55"
                    class="satellite satellite--1" />
            <!-- 小卫星 2 -->
            <circle cx="86" cy="140" r="5" fill="var(--accent)" opacity="0.3"
                    class="satellite satellite--2" />
          </g>

          <!-- 星星群 -->
          <g class="stars">
            <circle cx="42" cy="32" r="2.5" fill="var(--accent)" opacity="0.5" class="star star--1" />
            <circle cx="80" cy="56" r="1.2" fill="var(--accent)" opacity="0.3" class="star star--2" />
            <circle cx="260" cy="28" r="2" fill="var(--accent)" opacity="0.45" class="star star--3" />
            <circle cx="282" cy="52" r="1.5" fill="var(--accent)" opacity="0.25" class="star star--4" />
            <circle cx="58" cy="160" r="1.5" fill="var(--accent)" opacity="0.3" class="star star--5" />
            <circle cx="270" cy="150" r="2" fill="var(--accent)" opacity="0.35" class="star star--6" />
            <circle cx="22" cy="90" r="1" fill="var(--accent)" opacity="0.2" class="star star--7" />
            <circle cx="298" cy="100" r="1.2" fill="var(--accent)" opacity="0.25" class="star star--8" />
            <circle cx="110" cy="20" r="1.8" fill="var(--accent)" opacity="0.35" class="star star--1" />
            <circle cx="210" cy="16" r="1.3" fill="var(--accent)" opacity="0.3" class="star star--3" />
          </g>

          <!-- 十字星装饰 -->
          <g class="cross-stars">
            <g transform="translate(30, 75)" class="star star--2">
              <line x1="-4" y1="0" x2="4" y2="0" stroke="var(--accent)" stroke-width="1" opacity="0.3" />
              <line x1="0" y1="-4" x2="0" y2="4" stroke="var(--accent)" stroke-width="1" opacity="0.3" />
            </g>
            <g transform="translate(290, 130)" class="star star--5">
              <line x1="-3" y1="0" x2="3" y2="0" stroke="var(--accent)" stroke-width="0.8" opacity="0.25" />
              <line x1="0" y1="-3" x2="0" y2="3" stroke="var(--accent)" stroke-width="0.8" opacity="0.25" />
            </g>
          </g>

          <!-- 路标 -->
          <g class="sign" transform="translate(40, 140)">
            <rect x="12" y="0" width="3" height="62" rx="1.5" fill="var(--text-dim)" opacity="0.2" />
            <rect x="-6" y="6" width="42" height="22" rx="5" fill="var(--accent)" opacity="0.12" />
            <rect x="-6" y="6" width="42" height="22" rx="5" fill="none"
                  stroke="var(--accent)" stroke-width="0.6" opacity="0.2" />
            <text x="15" y="21" text-anchor="middle"
                  font-size="10" fill="var(--accent)" font-weight="700" opacity="0.7">?</text>
          </g>

          <!-- 渐变定义 -->
          <defs>
            <radialGradient id="planetGrad" cx="35%" cy="30%" r="65%">
              <stop offset="0%" stop-color="rgba(255,255,255,0.35)" />
              <stop offset="100%" stop-color="rgba(0,0,0,0.06)" />
            </radialGradient>
          </defs>
        </svg>
      </div>

      <!-- 错误码卡片 -->
      <div class="error-card">
        <div class="error-code">
          <span class="digit">4</span>
          <span class="digit digit--accent">
            <span class="digit-ring"></span>
            0
          </span>
          <span class="digit">4</span>
        </div>

        <!-- 分割线 -->
        <div class="divider">
          <span class="divider-dot"></span>
          <span class="divider-line"></span>
          <span class="divider-diamond"></span>
          <span class="divider-line"></span>
          <span class="divider-dot"></span>
        </div>

        <!-- 文字信息 -->
        <h1 class="title">页面不存在</h1>
        <p class="subtitle">你访问的页面可能已被移除、更名，或暂时不可用</p>
        <p class="subtitle-hint">请检查访问地址是否正确，或通过下方按钮返回</p>

        <!-- 按钮 -->
        <div class="actions">
          <el-button type="primary" class="btn-primary" @click="goHome">
            <span class="btn-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </span>
            返回首页
          </el-button>
          <el-button class="btn-ghost" @click="goBack">
            <span class="btn-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                   stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12" />
                <polyline points="12 19 5 12 12 5" />
              </svg>
            </span>
            返回上页
          </el-button>
        </div>
      </div>
    </div>

    <!-- 底部装饰 -->
    <div class="footer">
      <span class="footer-text">Error 404 · Page Not Found</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const sceneRef = ref(null)
const mouseX = ref(0.5)
const mouseY = ref(0.5)

const onMouseMove = (e) => {
  const rect = sceneRef.value?.getBoundingClientRect()
  if (!rect) return
  mouseX.value = (e.clientX - rect.left) / rect.width
  mouseY.value = (e.clientY - rect.top) / rect.height
}

const illustrationStyle = computed(() => {
  const dx = (mouseX.value - 0.5) * 8
  const dy = (mouseY.value - 0.5) * 6
  return { transform: `translate(${dx}px, ${dy}px)` }
})

const rayStyle = computed(() => {
  const dx = (mouseX.value - 0.5) * -12
  const dy = (mouseY.value - 0.5) * -10
  return { transform: `translate(${dx}px, ${dy}px)` }
})

const goHome = () => router.push('/')
const goBack = () => router.back()
</script>

<style scoped>
/* ── 变量 ── */
.not-found {
  --bg: #f8f5f0;
  --bg-warm: #f3ede5;
  --accent: #e07a4f;
  --accent-deep: #c96a42;
  --accent-soft: rgba(224, 122, 79, 0.1);
  --accent-glow: rgba(224, 122, 79, 0.2);
  --planet-fill: #eae0d5;
  --text-primary: #3d3229;
  --text-body: #6b5e52;
  --text-dim: #b5a99a;
  --card-bg: rgba(255, 253, 251, 0.65);
  --card-border: rgba(107, 94, 82, 0.07);
  --shadow-card: 0 1px 2px rgba(61, 50, 41, 0.03),
  0 4px 16px rgba(61, 50, 41, 0.05),
  0 16px 48px rgba(61, 50, 41, 0.04);
  --shadow-btn: 0 2px 12px rgba(224, 122, 79, 0.2);

  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 24px;
  background: var(--bg);
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif;
}

/* ── 纸张纹理 ── */
.texture {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.028;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 200px 200px;
}

/* ── 背景色块 ── */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
}

.bg-blob--1 {
  width: 520px;
  height: 520px;
  top: -18%;
  left: -12%;
  background: rgba(224, 122, 79, 0.08);
  animation: blobDrift 20s ease-in-out infinite;
}

.bg-blob--2 {
  width: 440px;
  height: 440px;
  bottom: -15%;
  right: -10%;
  background: rgba(168, 130, 100, 0.08);
  animation: blobDrift 24s 3s ease-in-out infinite reverse;
}

.bg-blob--3 {
  width: 300px;
  height: 300px;
  top: 45%;
  left: 50%;
  background: rgba(224, 122, 79, 0.05);
  animation: blobDrift 18s 6s ease-in-out infinite;
}

@keyframes blobDrift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(30px, -25px) scale(1.04); }
  66%      { transform: translate(-20px, 18px) scale(0.96); }
}

/* ── 浮动粒子 ── */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: var(--accent);
  opacity: 0;
  animation: particleFloat 16s ease-in-out infinite;
}

.particle--1  { width: 4px; height: 4px; top: 12%; left: 8%;  animation-delay: 0s; }
.particle--2  { width: 3px; height: 3px; top: 28%; right: 15%; animation-delay: 2s; }
.particle--3  { width: 5px; height: 5px; top: 65%; left: 5%;  animation-delay: 4s; }
.particle--4  { width: 3px; height: 3px; top: 78%; right: 20%; animation-delay: 6s; }
.particle--5  { width: 2px; height: 2px; top: 40%; left: 22%; animation-delay: 1s; }
.particle--6  { width: 4px; height: 4px; top: 55%; right: 8%;  animation-delay: 3s; }
.particle--7  { width: 2px; height: 2px; top: 18%; left: 45%; animation-delay: 7s; }
.particle--8  { width: 3px; height: 3px; top: 85%; left: 35%; animation-delay: 5s; }
.particle--9  { width: 2px; height: 2px; top: 35%; right: 35%; animation-delay: 8s; }
.particle--10 { width: 4px; height: 4px; top: 90%; right: 40%; animation-delay: 2.5s; }
.particle--11 { width: 3px; height: 3px; top: 5%;  right: 30%; animation-delay: 9s; }
.particle--12 { width: 2px; height: 2px; top: 48%; left: 12%; animation-delay: 4.5s; }

@keyframes particleFloat {
  0%   { opacity: 0; transform: translateY(0) scale(0); }
  10%  { opacity: 0.4; transform: translateY(-10px) scale(1); }
  90%  { opacity: 0.15; transform: translateY(-80px) scale(0.6); }
  100% { opacity: 0; transform: translateY(-100px) scale(0); }
}

/* ── 光线装饰 ── */
.rays {
  position: absolute;
  top: -20%;
  right: -5%;
  pointer-events: none;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.ray {
  position: absolute;
  background: linear-gradient(180deg, rgba(224, 122, 79, 0.06), transparent);
  border-radius: 2px;
  transform-origin: top center;
}

.ray--1 {
  width: 2px;
  height: 280px;
  top: 0;
  left: 0;
  transform: rotate(-15deg);
  animation: rayPulse 6s ease-in-out infinite;
}

.ray--2 {
  width: 1.5px;
  height: 200px;
  top: 20px;
  left: 40px;
  transform: rotate(-8deg);
  animation: rayPulse 6s 2s ease-in-out infinite;
}

.ray--3 {
  width: 1px;
  height: 160px;
  top: 40px;
  left: 70px;
  transform: rotate(-4deg);
  animation: rayPulse 6s 4s ease-in-out infinite;
}

@keyframes rayPulse {
  0%, 100% { opacity: 0.3; }
  50%      { opacity: 0.8; }
}

/* ── 主内容 ── */
.content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 480px;
  width: 100%;
}

/* ── 插画 ── */
.illustration {
  margin-bottom: 8px;
  animation: floatIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.scene {
  width: clamp(220px, 55vw, 320px);
  height: auto;
}

.planet-group {
  animation: planetFloat 6s ease-in-out infinite;
}

.planet-glow {
  animation: glowPulse 4s ease-in-out infinite;
}

@keyframes planetFloat {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-10px); }
}



.far-planet {
  animation: farFloat 8s ease-in-out infinite;
}

@keyframes farFloat {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(3px, -5px); }
}

.orbit-ring--1 {
  animation: ringSpin 14s linear infinite;
  transform-origin: 160px 112px;
}

.orbit-ring--2 {
  animation: ringSpin 10s linear infinite reverse;
  transform-origin: 160px 112px;
}

@keyframes ringSpin {
  to { transform: rotate(360deg); }
}

.satellite--1 {
  animation: sat1Orbit 7s ease-in-out infinite;
}

.satellite--2 {
  animation: sat2Orbit 9s ease-in-out infinite;
}

@keyframes sat1Orbit {
  0%, 100% { transform: translate(0, 0); }
  25%      { transform: translate(5px, -8px); }
  50%      { transform: translate(10px, 0); }
  75%      { transform: translate(5px, 8px); }
}

@keyframes sat2Orbit {
  0%, 100% { transform: translate(0, 0); }
  25%      { transform: translate(-4px, -5px); }
  50%      { transform: translate(-8px, 0); }
  75%      { transform: translate(-4px, 5px); }
}

/* 星星闪烁 */
.star--1 { animation: twinkle 3s ease-in-out infinite; }
.star--2 { animation: twinkle 2.5s 0.5s ease-in-out infinite; }
.star--3 { animation: twinkle 3.5s 1s ease-in-out infinite; }
.star--4 { animation: twinkle 2.8s 1.5s ease-in-out infinite; }
.star--5 { animation: twinkle 3.2s 0.8s ease-in-out infinite; }
.star--6 { animation: twinkle 2.6s 1.2s ease-in-out infinite; }
.star--7 { animation: twinkle 4s 2s ease-in-out infinite; }
.star--8 { animation: twinkle 3.8s 0.3s ease-in-out infinite; }

@keyframes twinkle {
  0%, 100% { opacity: 0.15; transform: scale(0.8); }
  50%      { opacity: 0.85; transform: scale(1.5); }
}

/* ── 错误码卡片 ── */
.error-card {
  background: var(--card-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--card-border);
  border-radius: 20px;
  padding: 32px 36px 36px;
  box-shadow: var(--shadow-card);
  animation: cardIn 0.8s 0.15s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  position: relative;
  overflow: hidden;
}

.error-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 5%, rgba(255,255,255,0.8) 30%, rgba(224,122,79,0.15) 50%, rgba(255,255,255,0.8) 70%, transparent 95%);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ── 404 数字 ── */
.error-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 24px;
}

.digit {
  font-size: clamp(52px, 10vw, 76px);
  font-weight: 800;
  color: var(--text-dim);
  line-height: 1;
  letter-spacing: -0.03em;
  font-family: 'Georgia', 'Noto Serif SC', serif;
  opacity: 0;
  animation: digitReveal 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.digit:nth-child(1) { animation-delay: 0.3s; }
.digit:nth-child(2) { animation-delay: 0.45s; }
.digit:nth-child(3) { animation-delay: 0.6s; }

@keyframes digitReveal {
  from { opacity: 0; transform: translateY(20px) scale(0.85); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.digit--accent {
  color: var(--accent);
  position: relative;
  margin: 0 6px;
}

.digit-ring {
  position: absolute;
  inset: -6px;
  border: 2px solid var(--accent-glow);
  border-radius: 50%;
  animation: digitRingSpin 8s linear infinite;
}

@keyframes digitRingSpin {
  to { transform: rotate(360deg); }
}

/* ── 分割线 ── */
.divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 22px;
  opacity: 0;
  animation: fadeScale 0.6s 0.7s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.divider-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--accent);
  opacity: 0.4;
}

.divider-line {
  width: 32px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  opacity: 0.4;
}

.divider-diamond {
  width: 6px;
  height: 6px;
  background: var(--accent);
  transform: rotate(45deg);
  opacity: 0.5;
  flex-shrink: 0;
}

@keyframes fadeScale {
  from { opacity: 0; transform: scaleX(0); }
  to   { opacity: 1; transform: scaleX(1); }
}

/* ── 文字 ── */
.title {
  margin: 0 0 10px;
  font-size: clamp(18px, 3.2vw, 24px);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.06em;
  opacity: 0;
  animation: slideUp 0.6s 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.7;
  opacity: 0;
  animation: slideUp 0.6s 0.9s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.subtitle-hint {
  margin: 4px 0 28px;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1.6;
  opacity: 0;
  animation: slideUp 0.6s 1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

/* ── 按钮组 ── */
.actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  opacity: 0;
  animation: slideUp 0.6s 1.1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  margin-right: 6px;
}

.btn-primary {
  --el-button-bg-color: var(--accent) !important;
  --el-button-border-color: var(--accent) !important;
  --el-button-text-color: #fff !important;
  --el-button-hover-bg-color: var(--accent-deep) !important;
  --el-button-hover-border-color: var(--accent-deep) !important;
  --el-button-hover-text-color: #fff !important;
  height: 44px;
  padding: 0 30px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 0.04em;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: var(--shadow-btn);
  position: relative;
  overflow: hidden;
}

.btn-primary::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(224, 122, 79, 0.35);
}

.btn-primary:hover::after {
  left: 100%;
}

.btn-primary:active {
  transform: translateY(0);
}

.btn-ghost {
  --el-button-bg-color: transparent !important;
  --el-button-border-color: rgba(107, 94, 82, 0.15) !important;
  --el-button-text-color: var(--text-body) !important;
  --el-button-hover-bg-color: var(--accent-soft) !important;
  --el-button-hover-border-color: var(--accent-glow) !important;
  --el-button-hover-text-color: var(--accent) !important;
  height: 44px;
  padding: 0 26px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-ghost:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(224, 122, 79, 0.1);
}

.btn-ghost:active {
  transform: translateY(0);
}

/* ── 底部 ── */
.footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  opacity: 0;
  animation: slideUp 0.6s 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.footer-text {
  font-size: 11px;
  color: var(--text-dim);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-family: 'Georgia', serif;
  opacity: 0.6;
}

/* ── 入场动画 ── */
@keyframes floatIn {
  from { opacity: 0; transform: translateY(28px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 响应式 ── */
@media (max-width: 640px) {
  .not-found {
    padding: 24px 16px;
  }

  .error-card {
    padding: 24px 20px 28px;
    border-radius: 16px;
  }

  .ray {
    display: none;
  }
}
</style>
