<template>
  <div class="intro" :class="{ 'intro--scrolled': scrolled }">
    <header class="intro-nav">
      <a class="nav-brand" href="#top" @click.prevent="scrollTop">
        <BrandMarkView :size="28" class="nav-mark" />
        <span class="nav-name">RapidKit</span>
      </a>
      <nav class="nav-links" aria-label="页面导航">
        <a href="#capabilities" class="nav-link">能力</a>
        <a href="#stack" class="nav-link">技术栈</a>
        <router-link class="nav-cta" to="/auth">进入系统</router-link>
      </nav>
    </header>

    <section id="top" class="hero" aria-labelledby="hero-brand">
      <img
        class="hero-img"
        src="/IntroHero.png?v=3"
        alt=""
        aria-hidden="true"
        draggable="false"
      />
      <div class="hero-veil" aria-hidden="true" />
      <div class="hero-glow" aria-hidden="true" />
      <div class="hero-copy">
        <h1 id="hero-brand" class="hero-brand">RapidKit</h1>
        <p class="hero-lead">
          前后端分离的通用业务底座，把认证、权限与系统管理先搭好，业务从这里往上长。
        </p>
        <div class="hero-actions">
          <router-link class="btn-primary" to="/auth">开始使用</router-link>
          <a class="btn-ghost" href="#capabilities">查看能力</a>
        </div>
      </div>
    </section>

    <section id="capabilities" class="band band--caps" aria-labelledby="caps-title">
      <div class="band-inner band-inner--caps">
        <div class="band-head">
          <h2 id="caps-title" class="band-title">开箱即用的底座能力</h2>
          <p class="band-sub">四块核心能力，覆盖从登录到菜单下发与 AI 对话的常见路径。</p>
        </div>
        <ul class="cap-list">
          <li v-for="item in capabilities" :key="item.title" class="cap-row">
            <span class="cap-name">{{ item.title }}</span>
            <span class="cap-desc">{{ item.desc }}</span>
          </li>
        </ul>
      </div>
    </section>

    <section id="stack" class="band band--stack" aria-labelledby="stack-title">
      <div class="band-inner">
        <div class="stack-head">
          <h2 id="stack-title" class="band-title band-title--on-dark">技术栈</h2>
          <p class="band-sub band-sub--on-dark">
            业务模块可直接复用认证与 RBAC，不必从零搭脚手架。
          </p>
        </div>
        <p class="stack-flow" aria-label="技术组成">
          <template v-for="(tech, i) in stack" :key="tech">
            <span class="stack-tech">{{ tech }}</span>
            <span v-if="i < stack.length - 1" class="stack-sep" aria-hidden="true">/</span>
          </template>
        </p>
      </div>
    </section>

    <section class="band band--cta" aria-labelledby="cta-title">
      <div class="band-inner band-inner--cta">
        <h2 id="cta-title" class="cta-title">准备好进入系统了吗？</h2>
        <p class="cta-sub">登录或注册后，按权限动态加载菜单。</p>
        <router-link class="btn-primary btn-primary--lg" to="/auth">进入 RapidKit</router-link>
      </div>
    </section>

    <footer class="intro-foot">
      <span class="foot-brand">RapidKit</span>
      <span class="foot-meta">v1.0 · 通用业务底座</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
/** 项目介绍页：品牌优先的落地介绍 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import BrandMarkView from '@/components/BrandMarkView.vue'

const scrolled = ref(false)

const capabilities = [
  {
    title: '认证',
    desc: '注册登录、图形验证码、Token 鉴权与登录后菜单下发。',
  },
  {
    title: '权限',
    desc: 'RBAC 角色与权限树，目录 / 菜单 / 功能分级，接口级校验。',
  },
  {
    title: '系统管理',
    desc: '用户与角色、个人信息、改密与头像上传。',
  },
  {
    title: 'AI 对话',
    desc: '流式对话、会话列表与历史记录，可按业务继续扩展。',
  },
]

const stack = [
  'Java 17',
  'Spring Boot 3',
  'Sa-Token',
  'MyBatis-Plus',
  'Vue 3',
  'Element Plus',
  'Redis',
  'MySQL',
]

function onScroll() {
  scrolled.value = window.scrollY > 12
}

function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.intro {
  --intro-ink: #0c2a42;
  --intro-night: #04141c;
  --intro-cyan: #0891b2;
  --intro-cyan-deep: #0e7490;
  --intro-glow: #5eead4;
  --intro-ice: #e4f1f5;
  --intro-mist: #f2f8fa;
  --intro-steel: #4d7080;
  --intro-line: rgba(8, 145, 178, 0.18);
  --intro-font-display: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  --intro-font-body: 'Noto Sans SC', 'DM Sans', var(--font-family-sans, sans-serif);

  min-height: 100dvh;
  color: var(--intro-ink);
  font-family: var(--intro-font-body);
  background: var(--intro-ice);
}

.intro-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px clamp(20px, 4vw, 48px);
  background: rgba(228, 241, 245, 0.55);
  backdrop-filter: blur(14px) saturate(150%);
  border-bottom: 1px solid transparent;
  transition:
    background 220ms ease,
    border-color 220ms ease,
    box-shadow 220ms ease;
}

.intro--scrolled .intro-nav {
  background: rgba(228, 241, 245, 0.92);
  border-bottom-color: var(--intro-line);
  box-shadow: 0 8px 28px rgba(4, 20, 28, 0.06);
}

.nav-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--intro-ink);
}

.nav-mark {
  color: var(--intro-cyan);
}

.nav-name {
  font-family: var(--intro-font-display);
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: clamp(12px, 2vw, 24px);
}

.nav-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--intro-steel);
  text-decoration: none;
  transition: color 150ms ease;
}

.nav-link:hover {
  color: var(--intro-cyan);
}

.nav-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  text-decoration: none;
  background: var(--intro-cyan);
  transition:
    background 150ms ease,
    transform 150ms ease;
}

.nav-cta:hover {
  background: var(--intro-cyan-deep);
  transform: translateY(-1px);
}

.nav-link:focus-visible,
.nav-cta:focus-visible,
.nav-brand:focus-visible,
.btn-primary:focus-visible,
.btn-ghost:focus-visible {
  outline: 2px solid var(--intro-cyan);
  outline-offset: 3px;
}

.hero {
  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding:
    calc(72px + env(safe-area-inset-top, 0px))
    clamp(20px, 5vw, 64px)
    48px;
  overflow: hidden;
}

.hero-img {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: left center;
  user-select: none;
  pointer-events: none;
  transform: scale(1.04);
  animation: hero-zoom 1.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.hero-veil {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    105deg,
    transparent 0%,
    transparent 32%,
    rgba(242, 248, 250, 0.55) 58%,
    rgba(242, 248, 250, 0.92) 74%,
    #f2f8fa 100%
  );
}

.hero-glow {
  display: none;
}

.hero-copy {
  position: relative;
  z-index: 2;
  width: min(100%, 440px);
  margin-right: clamp(0px, 4vw, 48px);
  padding: 8px 0 8px 4px;
  animation: hero-in 700ms cubic-bezier(0.16, 1, 0.3, 1) 80ms both;
}

.hero-brand {
  margin: 0;
  font-family: var(--intro-font-display);
  font-size: clamp(3.2rem, 8.5vw, 5.6rem);
  font-weight: 700;
  letter-spacing: -0.05em;
  line-height: 0.92;
  color: var(--intro-ink);
}

.hero-lead {
  margin: 22px 0 0;
  max-width: 22em;
  font-size: clamp(15px, 1.6vw, 17px);
  font-weight: 600;
  line-height: 1.65;
  color: #1a3a4d;
  text-wrap: pretty;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 30px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 22px;
  border-radius: 10px;
  font-family: var(--intro-font-display);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #fff;
  text-decoration: none;
  background: linear-gradient(135deg, var(--intro-cyan) 0%, var(--intro-cyan-deep) 100%);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.18) inset,
    0 12px 28px rgba(8, 145, 178, 0.32);
  transition:
    filter 150ms ease,
    transform 150ms ease,
    box-shadow 200ms ease;
}

.btn-primary:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.22) inset,
    0 16px 34px rgba(8, 145, 178, 0.4);
}

.btn-primary--lg {
  min-height: 52px;
  padding: 0 28px;
  font-size: 16px;
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 650;
  color: var(--intro-cyan-deep);
  text-decoration: none;
  background: #fff;
  border: 1px solid rgba(8, 145, 178, 0.32);
  transition:
    border-color 150ms ease,
    background 150ms ease;
}

.btn-ghost:hover {
  border-color: rgba(8, 145, 178, 0.55);
  background: #f2f8fa;
}

.band {
  position: relative;
  padding: clamp(64px, 10vh, 108px) clamp(20px, 5vw, 64px);
}

.band--caps {
  background: #fff;
  border-top: 1px solid var(--intro-line);
}

.band--stack {
  background:
    radial-gradient(ellipse 80% 60% at 100% 0%, rgba(8, 145, 178, 0.28) 0%, transparent 55%),
    radial-gradient(ellipse 50% 40% at 0% 100%, rgba(94, 234, 212, 0.12) 0%, transparent 50%),
    var(--intro-night);
  border-top: 1px solid rgba(94, 234, 212, 0.12);
}

.band--cta {
  background:
    radial-gradient(ellipse 70% 80% at 50% 100%, rgba(8, 145, 178, 0.1) 0%, transparent 60%),
    linear-gradient(180deg, #dceef4 0%, var(--intro-ice) 100%);
  border-top: 1px solid var(--intro-line);
  text-align: center;
}

.band-inner {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
}

.band-inner--cta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  max-width: 28em;
}

.band-inner--caps {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: clamp(32px, 6vw, 72px);
  align-items: start;
}

.band-head {
  max-width: 18em;
  position: sticky;
  top: 96px;
}

.band-title {
  margin: 0;
  font-family: var(--intro-font-display);
  font-size: clamp(1.65rem, 3.2vw, 2.2rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
  color: var(--intro-ink);
}

.band-title--on-dark {
  color: #e8f7fa;
}

.band-sub {
  margin: 12px 0 0;
  max-width: 36em;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.65;
  color: var(--intro-steel);
}

.band-sub--on-dark {
  color: rgba(196, 224, 232, 0.78);
}

.cap-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--intro-line);
}

.cap-row {
  display: grid;
  grid-template-columns: minmax(5.5rem, 7.5rem) minmax(0, 1fr);
  gap: clamp(16px, 3vw, 28px);
  align-items: baseline;
  padding: 22px 0;
  border-bottom: 1px solid var(--intro-line);
}

.cap-name {
  font-family: var(--intro-font-display);
  font-size: clamp(1.15rem, 2vw, 1.35rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--intro-cyan-deep);
}

.cap-desc {
  font-size: 15px;
  font-weight: 500;
  line-height: 1.65;
  color: var(--intro-steel);
  max-width: 32em;
}

.stack-head {
  max-width: 32em;
}

.stack-flow {
  margin: 36px 0 0;
  max-width: 46em;
  font-family: var(--intro-font-display);
  font-size: clamp(15px, 2vw, 18px);
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.85;
  color: #c8eef4;
}

.stack-tech {
  color: #e8f7fa;
}

.stack-sep {
  margin: 0 0.45em;
  color: rgba(94, 234, 212, 0.45);
  font-weight: 500;
}

.cta-title {
  margin: 0;
  font-family: var(--intro-font-display);
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--intro-ink);
}

.cta-sub {
  margin: 0 0 8px;
  font-size: 15px;
  color: var(--intro-steel);
}

.intro-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 20px clamp(20px, 5vw, 64px) calc(20px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--intro-line);
  background: #fff;
}

.foot-brand {
  font-family: var(--intro-font-display);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--intro-ink);
}

.foot-meta {
  font-family: var(--font-family-mono, monospace);
  font-size: 12px;
  color: var(--intro-steel);
}

@keyframes hero-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes hero-zoom {
  from {
    transform: scale(1.08);
    opacity: 0.85;
  }
  to {
    transform: scale(1.04);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-copy,
  .hero-img {
    animation: none;
  }

  .btn-primary:hover,
  .nav-cta:hover {
    transform: none;
  }
}

@media (max-width: 900px) {
  .hero {
    align-items: flex-end;
    justify-content: stretch;
    min-height: min(100dvh, 820px);
    padding-bottom: 40px;
  }

  .hero-img {
    object-position: 28% center;
  }

  .hero-veil {
    background: linear-gradient(
      180deg,
      rgba(4, 20, 28, 0.12) 0%,
      rgba(242, 248, 250, 0.45) 42%,
      rgba(242, 248, 250, 0.92) 72%,
      #f2f8fa 100%
    );
  }

  .hero-copy {
    width: 100%;
    margin-right: 0;
  }

  .band-inner--caps {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .band-head {
    position: static;
    max-width: 36em;
  }
}

@media (max-width: 560px) {
  .nav-links .nav-link {
    display: none;
  }

  .cap-row {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 18px 0;
  }

  .hero-brand {
    font-size: 2.75rem;
  }
}
</style>
