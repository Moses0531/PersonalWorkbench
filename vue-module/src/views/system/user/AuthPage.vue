<template>
  <div class="auth-page">
    <div class="auth-backdrop" aria-hidden="true">
      <!-- 流体效能：极光底色 -->
      <div class="auth-aurora" />
      <div class="auth-aurora-orb auth-aurora-orb--a" />
      <div class="auth-aurora-orb auth-aurora-orb--b" />
      <div class="auth-aurora-orb auth-aurora-orb--c" />
      <div class="auth-desk" />

      <!-- 贝塞尔流线：连接日程 / 任务节点 -->
      <svg class="auth-flow" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" fill="none">
        <path
          class="auth-flow__path"
          d="M80 620C260 480 340 720 520 540C700 360 820 640 1040 420C1180 280 1280 360 1420 240"
          stroke="url(#flowGradA)"
          stroke-width="1.8"
          stroke-linecap="round"
        />
        <path
          class="auth-flow__path auth-flow__path--b"
          d="M40 220C280 320 420 60 640 200C860 340 980 80 1220 180C1320 220 1380 160 1460 120"
          stroke="url(#flowGradB)"
          stroke-width="1.4"
          stroke-linecap="round"
        />
        <path
          class="auth-flow__path auth-flow__path--c"
          d="M200 780C380 700 520 820 720 680C920 540 1080 760 1280 620"
          stroke="rgba(8,145,178,0.14)"
          stroke-width="1.2"
          stroke-linecap="round"
        />
        <circle class="auth-flow__node" cx="520" cy="540" r="4" fill="rgba(8,145,178,0.45)" />
        <circle class="auth-flow__node auth-flow__node--b" cx="1040" cy="420" r="3.5" fill="rgba(34,211,238,0.55)" />
        <circle class="auth-flow__node auth-flow__node--c" cx="640" cy="200" r="3" fill="rgba(14,116,144,0.4)" />
        <defs>
          <linearGradient id="flowGradA" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(8,145,178,0.05)" />
            <stop offset="45%" stop-color="rgba(34,211,238,0.35)" />
            <stop offset="100%" stop-color="rgba(8,145,178,0.08)" />
          </linearGradient>
          <linearGradient id="flowGradB" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="rgba(14,116,144,0.06)" />
            <stop offset="50%" stop-color="rgba(8,145,178,0.22)" />
            <stop offset="100%" stop-color="rgba(34,211,238,0.08)" />
          </linearGradient>
        </defs>
      </svg>

      <!-- 抽象几何：同心环=日程 · 磨砂卡=任务 -->
      <div class="auth-geo">
        <div class="geo-rings geo-rings--a">
          <span /><span /><span /><span />
        </div>
        <div class="geo-rings geo-rings--b">
          <span /><span /><span />
        </div>
        <div class="geo-stack geo-stack--a">
          <div class="geo-card" style="--i: 0">
            <span class="geo-card__label">今日</span>
            <strong class="geo-card__value">3</strong>
            <span class="geo-card__hint">待办</span>
          </div>
          <div class="geo-card" style="--i: 1">
            <span class="geo-card__label">本周</span>
            <span class="geo-card__bars">
              <i style="--h: 42%" /><i style="--h: 68%" /><i style="--h: 55%" /><i style="--h: 82%" />
            </span>
          </div>
          <div class="geo-card" style="--i: 2">
            <span class="geo-card__time">14:00</span>
            <span class="geo-card__hint">对齐会</span>
          </div>
        </div>
        <div class="geo-stack geo-stack--b">
          <div class="geo-card geo-card--slim" style="--i: 0">
            <span class="geo-card__slim-dot" />
            <span class="geo-card__slim-line" />
          </div>
          <div class="geo-card geo-card--slim" style="--i: 1">
            <span class="geo-card__slim-dot geo-card__slim-dot--on" />
            <span class="geo-card__slim-line" />
          </div>
          <div class="geo-card geo-card--slim" style="--i: 2">
            <span class="geo-card__slim-dot" />
            <span class="geo-card__slim-line geo-card__slim-line--short" />
          </div>
        </div>
        <div class="geo-morph geo-morph--a" />
        <div class="geo-morph geo-morph--b" />
        <div class="geo-pulse" />
      </div>

      <div class="auth-grain" />
    </div>

    <div class="auth-shell">
      <section class="auth-stage" aria-label="产品介绍">
        <header class="stage-head">
          <div class="stage-brand">
            <span class="stage-brand__mark">
              <BrandMarkView :size="40" />
            </span>
            <div class="stage-brand__text">
              <p class="stage-brand__en">Personal Workbench</p>
              <p class="stage-brand__zh">个人工作台</p>
            </div>
          </div>
          <h1 class="stage-title">推进今天的工作</h1>
        </header>

        <div class="stage-mock" aria-hidden="true">
          <div class="desk-scene">
            <div class="desk-photo" />
            <div class="desk-surface">
              <div class="desk-surface__shine" />
              <div class="desk-surface__edge" />
            </div>

            <div class="stage-floaters">
              <div class="prop prop--sticky">
                <span>14:00</span>
                <span>对齐会</span>
              </div>
              <div class="prop prop--sticky prop--sticky-b">
                <span>设计稿</span>
                <span>评审</span>
              </div>
              <div class="prop prop--chip prop--chip-todo">
                <span class="prop-chip__check" />
                <span>优先级</span>
              </div>
              <div class="prop prop--chip prop--chip-progress">
                <span class="prop-chip__bars">
                  <i style="--h: 42%" /><i style="--h: 68%" /><i style="--h: 55%" /><i style="--h: 82%" /><i style="--h: 70%" />
                </span>
              </div>
              <div class="prop prop--chip prop--chip-clock">
                <span class="prop-chip__ring" />
                <span class="prop-chip__time">09:24</span>
              </div>
              <div class="prop prop--chip prop--chip-mail">
                <span class="prop-chip__mail-icon" />
                <span>3</span>
              </div>
              <div class="prop prop--chip prop--chip-focus">
                <span class="prop-chip__pulse" />
                <span>专注 25:00</span>
              </div>
              <div class="prop prop--chip prop--chip-sync">
                <span class="prop-chip__cloud" />
                <span>已同步</span>
              </div>
              <div class="prop prop--chip prop--chip-ai">
                <span class="prop-chip__spark" />
                <span>AI 建议</span>
              </div>
            </div>

            <div class="desk-devices">
              <div class="mock-monitor">
                <div class="mock-monitor__frame">
                  <span class="mock-monitor__cam" />
                  <div class="mock-plane">
                    <div class="mock-pane mock-pane--todo" style="--i: 0">
                      <div class="mock-pane__head">
                        <span class="mock-pane__label">今日待办</span>
                        <span class="mock-pane__meta">3 项</span>
                      </div>
                      <ul class="mock-todo">
                        <li class="mock-todo__item is-done">
                          <span class="mock-check" />
                          <span class="mock-todo__text">整理本周优先级</span>
                        </li>
                        <li class="mock-todo__item">
                          <span class="mock-check" />
                          <span class="mock-todo__text">准备方案提纲</span>
                          <span class="mock-prio mock-prio--high" />
                        </li>
                        <li class="mock-todo__item">
                          <span class="mock-check" />
                          <span class="mock-todo__text">跟进项目进度</span>
                          <span class="mock-prio mock-prio--mid" />
                        </li>
                      </ul>
                    </div>

                    <div class="mock-pane mock-pane--cal" style="--i: 1">
                      <div class="mock-pane__head">
                        <span class="mock-pane__label">本周日程</span>
                        <span class="mock-pane__meta">周视图</span>
                      </div>
                      <div class="mock-week">
                        <div
                          v-for="day in weekDays"
                          :key="day.label"
                          class="mock-day"
                          :class="{ 'is-today': day.today }"
                        >
                          <span class="mock-day__label">{{ day.label }}</span>
                          <span class="mock-day__num">{{ day.num }}</span>
                          <span
                            v-for="(block, bi) in day.blocks"
                            :key="bi"
                            class="mock-block"
                            :class="block.tone"
                            :style="{ '--span': block.span }"
                          />
                        </div>
                      </div>
                      <div class="mock-event mock-event--pulse">
                        <span class="mock-event__time">14:00</span>
                        <span class="mock-event__title">需求对齐会</span>
                      </div>
                    </div>

                    <div class="mock-pane mock-pane--board" style="--i: 2">
                      <div class="mock-pane__head">
                        <span class="mock-pane__label">项目看板</span>
                        <span class="mock-pane__meta">可拖拽</span>
                      </div>
                      <div class="mock-board">
                        <div v-for="col in boardCols" :key="col.name" class="mock-col">
                          <span class="mock-col__name">{{ col.name }}</span>
                          <div
                            v-for="(card, ci) in col.cards"
                            :key="ci"
                            class="mock-card"
                            :style="{ width: card.w }"
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="mock-monitor__chin" />
                <div class="mock-monitor__neck" />
                <div class="mock-monitor__foot" />
                <div class="mock-monitor__shadow" />
              </div>

              <div class="mock-tablet">
                <div class="mock-tablet__bezel">
                  <div class="mock-tablet__screen">
                    <div class="mock-tablet__bar">
                      <span>今日</span>
                      <strong>3</strong>
                    </div>
                    <div class="mock-tablet__list">
                      <i /><i /><i />
                    </div>
                    <div class="mock-tablet__chip">14:00 对齐</div>
                  </div>
                </div>
                <div class="mock-tablet__shadow" />
              </div>
            </div>

            <div class="desk-props">
              <svg class="desk-prop desk-prop--coffee" viewBox="0 0 72 80" fill="none">
                <ellipse cx="32" cy="72" rx="22" ry="5" fill="rgba(6,36,64,0.18)" />
                <path d="M14 28h36c1.5 18-2 36-18 36S12.5 46 14 28Z" fill="rgba(255,255,255,0.97)" stroke="rgba(12,74,94,0.55)" stroke-width="1.8" />
                <path d="M50 34c8 1 12 8 10 14s-9 8-14 6" stroke="rgba(12,74,94,0.5)" stroke-width="1.8" stroke-linecap="round" />
                <path d="M18 28c1-8 8-12 14-12s13 4 14 12" fill="rgba(8,145,178,0.32)" />
                <path d="M28 10c0 4 2 5 2 8M36 8c0 5 2 6 2 10M42 11c0 3 1.5 4 1.5 7" stroke="rgba(14,116,144,0.55)" stroke-width="1.6" stroke-linecap="round" />
              </svg>
              <svg class="desk-prop desk-prop--notebook" viewBox="0 0 88 72" fill="none">
                <rect x="10" y="8" width="58" height="54" rx="4" fill="rgba(255,255,255,0.98)" stroke="rgba(12,74,94,0.5)" stroke-width="1.6" transform="rotate(-8 39 35)" />
                <path d="M22 20h34M22 30h28M22 40h32" stroke="rgba(8,145,178,0.4)" stroke-width="1.6" stroke-linecap="round" transform="rotate(-8 39 35)" />
                <rect x="48" y="18" width="28" height="36" rx="2" fill="rgba(34,211,238,0.32)" stroke="rgba(12,74,94,0.42)" stroke-width="1.3" transform="rotate(12 62 36)" />
              </svg>
              <svg class="desk-prop desk-prop--pen" viewBox="0 0 24 64" fill="none">
                <path d="M10 4l4 2 2 48-5 6-5-6 2-48z" fill="rgba(255,255,255,0.94)" stroke="rgba(12,74,94,0.5)" stroke-width="1.3" />
                <path d="M10 8h6" stroke="rgba(8,145,178,0.65)" stroke-width="1.3" />
                <path d="M11 54l1 6 1-6" fill="rgba(8,145,178,0.7)" />
              </svg>
              <svg class="desk-prop desk-prop--mouse" viewBox="0 0 48 64" fill="none">
                <rect x="10" y="6" width="28" height="48" rx="14" fill="rgba(255,255,255,0.96)" stroke="rgba(12,74,94,0.5)" stroke-width="1.6" />
                <path d="M24 12v10" stroke="rgba(8,145,178,0.55)" stroke-width="1.6" stroke-linecap="round" />
                <ellipse cx="24" cy="58" rx="12" ry="3" fill="rgba(6,36,64,0.14)" />
              </svg>
              <svg class="desk-prop desk-prop--plant" viewBox="0 0 56 72" fill="none">
                <ellipse cx="28" cy="66" rx="16" ry="4" fill="rgba(6,36,64,0.14)" />
                <path d="M18 66h20l-2-18H20l-2 18Z" fill="rgba(255,255,255,0.92)" stroke="rgba(12,74,94,0.45)" stroke-width="1.4" />
                <path d="M28 48c-8-10-14-18-6-28 2 8 6 14 6 28Z" fill="rgba(8,145,178,0.35)" />
                <path d="M28 48c8-12 14-20 8-30-4 8-7 16-8 30Z" fill="rgba(14,116,144,0.4)" />
                <path d="M28 48c0-14 2-24 10-32-2 10-6 20-10 32Z" fill="rgba(34,211,238,0.35)" />
              </svg>

              <!-- 手机 + 无线充 -->
              <div class="desk-prop desk-prop--phone">
                <span class="desk-phone__pad" />
                <span class="desk-phone__glow" />
                <svg class="desk-phone__device" viewBox="0 0 40 72" fill="none">
                  <rect x="4" y="2" width="32" height="68" rx="7" fill="rgba(255,255,255,0.97)" stroke="rgba(12,74,94,0.5)" stroke-width="1.5" />
                  <rect x="8" y="10" width="24" height="44" rx="2" fill="rgba(232,246,252,0.95)" />
                  <rect x="14" y="5" width="12" height="3" rx="1.5" fill="rgba(12,74,94,0.28)" />
                  <circle cx="20" cy="62" r="3" fill="rgba(8,145,178,0.35)" />
                  <path d="M11 16h18M11 22h14M11 28h16" stroke="rgba(8,145,178,0.4)" stroke-width="1.4" stroke-linecap="round" />
                </svg>
              </div>

              <!-- 无线耳机盒 -->
              <svg class="desk-prop desk-prop--buds" viewBox="0 0 56 40" fill="none">
                <ellipse cx="28" cy="36" rx="18" ry="3" fill="rgba(6,36,64,0.14)" />
                <rect x="6" y="10" width="44" height="24" rx="12" fill="rgba(255,255,255,0.97)" stroke="rgba(12,74,94,0.48)" stroke-width="1.5" />
                <path d="M8 22h40" stroke="rgba(8,145,178,0.22)" stroke-width="1.2" />
                <circle cx="20" cy="18" r="5" fill="rgba(232,246,252,0.95)" stroke="rgba(8,145,178,0.45)" stroke-width="1.2" />
                <circle cx="36" cy="18" r="5" fill="rgba(232,246,252,0.95)" stroke="rgba(8,145,178,0.45)" stroke-width="1.2" />
                <circle cx="28" cy="28" r="2" fill="rgba(34,211,238,0.55)" />
              </svg>

              <!-- USB-C 拓展坞 -->
              <svg class="desk-prop desk-prop--dock" viewBox="0 0 96 36" fill="none">
                <rect x="4" y="8" width="88" height="20" rx="6" fill="rgba(70,98,112,0.92)" stroke="rgba(12,42,58,0.45)" stroke-width="1.3" />
                <rect x="12" y="14" width="10" height="8" rx="2" fill="rgba(34,211,238,0.35)" />
                <rect x="28" y="14" width="10" height="8" rx="2" fill="rgba(255,255,255,0.2)" />
                <rect x="44" y="14" width="10" height="8" rx="2" fill="rgba(255,255,255,0.2)" />
                <rect x="60" y="15" width="14" height="6" rx="2" fill="rgba(8,145,178,0.45)" />
                <circle cx="84" cy="18" r="3" fill="rgba(74,186,106,0.7)" />
                <path d="M48 8V4" stroke="rgba(12,74,94,0.4)" stroke-width="1.4" stroke-linecap="round" />
              </svg>

              <!-- 智能音箱 -->
              <svg class="desk-prop desk-prop--speaker" viewBox="0 0 48 56" fill="none">
                <ellipse cx="24" cy="50" rx="16" ry="4" fill="rgba(6,36,64,0.14)" />
                <rect x="8" y="6" width="32" height="42" rx="14" fill="rgba(255,255,255,0.96)" stroke="rgba(12,74,94,0.48)" stroke-width="1.5" />
                <circle cx="24" cy="24" r="10" fill="rgba(232,246,252,0.9)" stroke="rgba(8,145,178,0.35)" stroke-width="1.2" />
                <circle cx="24" cy="24" r="4" fill="rgba(8,145,178,0.45)" />
                <path d="M16 40h16" stroke="rgba(8,145,178,0.3)" stroke-width="1.4" stroke-linecap="round" />
                <circle cx="24" cy="10" r="1.5" fill="rgba(34,211,238,0.75)" />
              </svg>

              <!-- 触控板 -->
              <svg class="desk-prop desk-prop--trackpad" viewBox="0 0 72 48" fill="none">
                <rect x="4" y="6" width="64" height="36" rx="8" fill="rgba(255,255,255,0.96)" stroke="rgba(12,74,94,0.48)" stroke-width="1.5" />
                <rect x="14" y="14" width="44" height="20" rx="4" fill="rgba(232,246,252,0.75)" stroke="rgba(8,145,178,0.25)" stroke-width="1" />
              </svg>
            </div>
          </div>
        </div>
      </section>

      <aside class="auth-rail" aria-label="登录区域">
        <div class="auth-rail__glow" aria-hidden="true" />
        <div class="auth-rail__props" aria-hidden="true">
          <svg class="prop prop--lamp" viewBox="0 0 96 120" fill="none">
            <path d="M48 108V62" stroke="rgba(12,74,94,0.5)" stroke-width="2.2" stroke-linecap="round" />
            <ellipse cx="48" cy="112" rx="18" ry="4" fill="rgba(6,36,64,0.12)" />
            <path
              d="M28 62c4-22 16-34 20-34s16 12 20 34H28Z"
              fill="rgba(255,248,230,0.96)"
              stroke="rgba(12,74,94,0.45)"
              stroke-width="1.6"
            />
            <path d="M34 62h28" stroke="rgba(8,145,178,0.45)" stroke-width="1.4" />
            <ellipse cx="48" cy="78" rx="28" ry="14" fill="rgba(255,220,140,0.32)" />
          </svg>

          <svg class="prop prop--keyboard" viewBox="0 0 160 64" fill="none">
            <rect x="4" y="10" width="152" height="44" rx="8" fill="rgba(255,255,255,0.96)" stroke="rgba(12,74,94,0.48)" stroke-width="1.8" />
            <g fill="rgba(8,145,178,0.2)" stroke="rgba(12,74,94,0.22)" stroke-width="0.8">
              <rect x="14" y="20" width="12" height="10" rx="2" />
              <rect x="30" y="20" width="12" height="10" rx="2" />
              <rect x="46" y="20" width="12" height="10" rx="2" />
              <rect x="62" y="20" width="12" height="10" rx="2" />
              <rect x="78" y="20" width="12" height="10" rx="2" />
              <rect x="94" y="20" width="12" height="10" rx="2" />
              <rect x="110" y="20" width="12" height="10" rx="2" />
              <rect x="126" y="20" width="18" height="10" rx="2" />
              <rect x="22" y="34" width="12" height="10" rx="2" />
              <rect x="38" y="34" width="12" height="10" rx="2" />
              <rect x="54" y="34" width="48" height="10" rx="2" />
              <rect x="106" y="34" width="12" height="10" rx="2" />
              <rect x="122" y="34" width="22" height="10" rx="2" />
            </g>
            <ellipse cx="80" cy="58" rx="48" ry="4" fill="rgba(6,36,64,0.1)" />
          </svg>

          <svg class="prop prop--plant" viewBox="0 0 56 72" fill="none">
            <ellipse cx="28" cy="66" rx="16" ry="4" fill="rgba(6,36,64,0.12)" />
            <path d="M20 64h16l-2-18H22l-2 18Z" fill="rgba(200,226,238,0.95)" stroke="rgba(12,74,94,0.45)" stroke-width="1.4" />
            <path d="M28 46c-8-10-16-12-18-8 6 2 12 8 18 12Z" fill="rgba(34,211,238,0.32)" stroke="rgba(12,74,94,0.4)" stroke-width="1.3" />
            <path d="M28 46c8-10 16-12 18-8-6 2-12 8-18 12Z" fill="rgba(8,145,178,0.22)" stroke="rgba(12,74,94,0.4)" stroke-width="1.3" />
            <path d="M28 46V28" stroke="rgba(12,74,94,0.45)" stroke-width="1.5" stroke-linecap="round" />
          </svg>

          <div class="prop prop--envelope" />
          <div class="prop prop--coaster" />
        </div>

        <div class="form-card">
        <Transition name="auth-slide-up" mode="out-in">
          <div :key="activeTab" class="auth-panel">
            <header class="auth-head">
              <h2 class="auth-title">{{ isLogin ? '欢迎回来' : '创建工作台' }}</h2>
              <p class="auth-sub">
                {{ isLogin ? '用账号继续今天的安排' : '三步注册，马上开工' }}
              </p>
            </header>

            <a-form
              v-if="isLogin"
              ref="loginFormRef"
              class="auth-form"
              :model="loginForm"
              :rules="loginRules"
              layout="vertical"
              size="large"
              @submit.prevent="handleLogin"
            >
              <a-form-item name="account">
                <template #label><span class="label-text">账号 / 手机号 / 邮箱</span></template>
                <a-input
                  v-model:value.trim="loginForm.account"
                  placeholder="请输入账号、手机号或邮箱"
                  allow-clear
                >
                  <template #prefix><UserOutlined /></template>
                </a-input>
              </a-form-item>

              <a-form-item name="password">
                <template #label><span class="label-text">密码</span></template>
                <a-input-password v-model:value.trim="loginForm.password" placeholder="请输入密码">
                  <template #prefix><LockOutlined /></template>
                </a-input-password>
              </a-form-item>

              <a-form-item v-if="captchaOnLogin" class="form-item-last">
                <template #label><span class="label-text">验证码</span></template>
                <div class="captcha-row">
                  <a-input
                    v-model:value.trim="loginForm.captcha"
                    placeholder="请输入验证码"
                    allow-clear
                  >
                    <template #prefix><CheckCircleOutlined /></template>
                  </a-input>
                  <button
                    type="button"
                    class="captcha-box"
                    title="点击刷新验证码"
                    @click="refreshCaptcha"
                  >
                    <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                    <span v-else class="captcha-fallback">刷新</span>
                  </button>
                </div>
              </a-form-item>
            </a-form>

            <a-form
              v-else-if="registerEnabled"
              class="auth-form"
              :model="registerForm"
              layout="vertical"
              size="large"
              @submit.prevent="handleRegister"
            >
              <a-form-item>
                <template #label><span class="label-text">手机号 / 邮箱</span></template>
                <a-input
                  v-model:value.trim="registerForm.contact"
                  placeholder="请输入手机号或邮箱"
                  allow-clear
                >
                  <template #prefix>
                    <MobileOutlined v-if="looksLikePhone(registerForm.contact)" />
                    <MailOutlined v-else-if="looksLikeEmail(registerForm.contact)" />
                    <UserOutlined v-else />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item>
                <template #label><span class="label-text">密码</span></template>
                <a-input-password
                  v-model:value.trim="registerForm.password"
                  placeholder="至少 6 位"
                >
                  <template #prefix><LockOutlined /></template>
                </a-input-password>
              </a-form-item>

              <a-form-item>
                <template #label><span class="label-text">确认密码</span></template>
                <a-input-password
                  v-model:value.trim="registerForm.confirmPassword"
                  placeholder="请再次输入密码"
                >
                  <template #prefix><LockOutlined /></template>
                </a-input-password>
              </a-form-item>

              <a-form-item v-if="captchaOnRegister" class="form-item-last">
                <template #label><span class="label-text">验证码</span></template>
                <div class="captcha-row">
                  <a-input
                    v-model:value.trim="registerForm.captcha"
                    placeholder="请输入验证码"
                    allow-clear
                  >
                    <template #prefix><CheckCircleOutlined /></template>
                  </a-input>
                  <button
                    type="button"
                    class="captcha-box"
                    title="点击刷新验证码"
                    @click="refreshCaptcha"
                  >
                    <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                    <span v-else class="captcha-fallback">刷新</span>
                  </button>
                </div>
              </a-form-item>
            </a-form>

            <div class="auth-foot">
              <div v-if="isLogin" class="auth-actions">
                <div class="option-row">
                  <a-checkbox v-model:checked="rememberMe">
                    <span class="check-label">记住我</span>
                  </a-checkbox>
                  <span class="link link--muted" title="功能即将开放">忘记密码？</span>
                </div>
                <a-button
                  class="submit-btn"
                  type="primary"
                  :loading="loginLoading"
                  @click="handleLogin"
                >
                  <span v-if="!loginLoading" class="btn-content">
                    <span>进入工作台</span>
                    <ArrowRightOutlined class="btn-arrow" />
                  </span>
                  <span v-else>正在登录...</span>
                </a-button>
                <p v-if="registerEnabled" class="switch-line">
                  还没有账号？
                  <a class="link" href="javascript:void(0)" @click="switchMode('register')">去注册</a>
                </p>
              </div>

              <div v-else-if="registerEnabled" class="auth-actions">
                <a-button
                  class="submit-btn"
                  type="primary"
                  :loading="registerLoading"
                  @click="handleRegister"
                >
                  <span v-if="!registerLoading" class="btn-content">
                    <span>立即注册</span>
                    <ArrowRightOutlined class="btn-arrow" />
                  </span>
                  <span v-else>正在注册...</span>
                </a-button>
                <p class="switch-line">
                  已有账号？
                  <a class="link" href="javascript:void(0)" @click="switchMode('login')">去登录</a>
                </p>
              </div>
            </div>
          </div>
        </Transition>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
/** 登录/注册页：流体效能背景（极光 + 几何流线）与办公锚点，对齐内页青色主题 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  LockOutlined,
  MobileOutlined,
  MailOutlined,
  CheckCircleOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons-vue'
import { loginApi, registerApi, fetchAuthConfigApi } from '@/apis/system/user/AuthApi'
import { fetchCaptchaBase64Api } from '@/apis/common/CaptchaApi'
import { useUserStore } from '@/stores/userStore'
import { buildRoutes, resolveDefaultPath } from '@/router/dynamicRoutes'
import appRouter from '@/router'
import BrandMarkView from '@/components/BrandMarkView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { setAuth, setUserLoginInfo } = userStore

const registerEnabled = ref(true)
const captchaOnLogin = ref(true)
const captchaOnRegister = ref(true)
const activeTab = ref(route.query.tab === 'register' ? 'register' : 'login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const rememberMe = ref(false)
const loginFormRef = ref(null)
const captchaBase64 = ref('')
const captchaToken = ref('')
const captchaTimestamp = ref(0)

const isLogin = computed(() => activeTab.value === 'login')

const weekDays = [
  { label: '一', num: '14', blocks: [{ tone: 'tone-a', span: 1 }] },
  { label: '二', num: '15', blocks: [{ tone: 'tone-b', span: 1 }] },
  { label: '三', num: '16', today: true, blocks: [{ tone: 'tone-a', span: 1 }, { tone: 'tone-c', span: 1 }] },
  { label: '四', num: '17', blocks: [{ tone: 'tone-b', span: 1 }] },
  { label: '五', num: '18', blocks: [] },
]

const boardCols = [
  { name: '待办', cards: [{ w: '100%' }, { w: '78%' }] },
  { name: '进行中', cards: [{ w: '92%' }] },
  { name: '已完成', cards: [{ w: '86%' }, { w: '64%' }] },
]

const loginForm = reactive({
  account: '',
  password: '',
  captcha: ''
})

const registerForm = reactive({
  contact: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_PATTERN = /^1[3-9]\d{9}$/

const loginRules = {
  account: [{ required: true, message: '请输入账号、手机号或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function looksLikePhone(value) {
  const text = value?.trim() || ''
  return text.length > 0 && /^\d+$/.test(text)
}

function looksLikeEmail(value) {
  const text = value?.trim() || ''
  return text.includes('@')
}

function needCaptchaForCurrentView() {
  return isLogin.value ? captchaOnLogin.value : captchaOnRegister.value
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'login') {
      activeTab.value = 'login'
    } else if (tab === 'register' && registerEnabled.value) {
      activeTab.value = 'register'
    } else if (tab === 'register' && !registerEnabled.value) {
      switchMode('login')
    }
  }
)

function switchMode(mode) {
  if (mode === 'register' && !registerEnabled.value) return
  if (activeTab.value === mode) return
  activeTab.value = mode
  if (mode === 'register') {
    registerForm.contact = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.captcha = ''
  }
  router.replace({ path: '/auth', query: { ...route.query, tab: mode } })
  if (needCaptchaForCurrentView()) {
    refreshCaptcha()
  }
}

async function refreshCaptcha() {
  try {
    const result = await fetchCaptchaBase64Api()
    captchaBase64.value = result.data.img
    captchaToken.value = result.data.token
    captchaTimestamp.value = result.data.timestamp
  } catch (error) {
    message.error(error.message || '获取验证码失败')
    captchaBase64.value = ''
  }
}

function resolveRegisterContact() {
  const contact = registerForm.contact?.trim() || ''
  if (!contact) {
    return { error: '请输入手机号或邮箱' }
  }
  if (PHONE_PATTERN.test(contact)) {
    return { phone: contact }
  }
  if (EMAIL_PATTERN.test(contact)) {
    return { email: contact }
  }
  if (/^\d+$/.test(contact)) {
    return { error: '请输入正确的手机号' }
  }
  return { error: '请输入正确的手机号或邮箱' }
}

function completeLogin(data) {
  if (!data?.token) {
    throw new Error('登录成功但未获取到令牌')
  }
  if (data?.userId == null) {
    throw new Error('登录成功但未获取到用户ID')
  }

  setAuth(data.token, {
    userId: data.userId,
    account: data.account,
    username: data.username,
    avatar: data.avatar,
  })
  setUserLoginInfo(data)
  buildRoutes(appRouter)
  message.success('登录成功')
  const redirect =
    typeof route.query.redirect === 'string' ? route.query.redirect : resolveDefaultPath('/dashboard')
  router.replace(redirect)
}

async function handleLogin() {
  if (captchaOnLogin.value && !loginForm.captcha?.trim()) {
    message.warning('请输入验证码')
    return
  }

  loginLoading.value = true
  try {
    const result = await loginApi({
      account: loginForm.account,
      password: loginForm.password,
      captchaCode: captchaOnLogin.value ? loginForm.captcha : undefined,
      captchaToken: captchaOnLogin.value ? captchaToken.value : undefined,
      captchaTimestamp: captchaOnLogin.value ? captchaTimestamp.value : undefined,
    })
    completeLogin(result?.data || {})
  } catch (error) {
    message.error(error.message || '登录失败')
    if (captchaOnLogin.value) {
      refreshCaptcha()
      loginForm.captcha = ''
    }
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  const contact = resolveRegisterContact()
  if (contact.error) {
    message.warning(contact.error)
    return
  }
  if (!registerForm.password?.trim()) {
    message.warning('请输入密码')
    return
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 20) {
    message.warning('密码长度需在 6-20 位之间')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    message.warning('两次输入密码不一致')
    return
  }
  if (captchaOnRegister.value && !registerForm.captcha?.trim()) {
    message.warning('请输入验证码')
    return
  }

  registerLoading.value = true
  try {
    const payload = {
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      captchaCode: captchaOnRegister.value ? registerForm.captcha : undefined,
      captchaToken: captchaOnRegister.value ? captchaToken.value : undefined,
      captchaTimestamp: captchaOnRegister.value ? captchaTimestamp.value : undefined,
      ...contact,
    }

    const result = await registerApi(payload)
    const data = result?.data || {}
    if (!data.account) {
      throw new Error('注册成功但未获取到系统账号')
    }
    message.success(`注册成功，系统账号：${data.account}`)
    loginForm.account = data.account
    registerForm.contact = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.captcha = ''
    switchMode('login')
  } catch (error) {
    message.error(error.message || '注册失败')
    if (captchaOnRegister.value) {
      refreshCaptcha()
      registerForm.captcha = ''
    }
  } finally {
    registerLoading.value = false
  }
}

async function loadAuthConfig() {
  try {
    const result = await fetchAuthConfigApi()
    const data = result?.data || {}
    registerEnabled.value = data.registerEnabled !== false
    captchaOnLogin.value = data.captchaOnLogin !== false
    captchaOnRegister.value = data.captchaOnRegister !== false
    if (!registerEnabled.value && activeTab.value === 'register') {
      activeTab.value = 'login'
      router.replace({ path: '/auth', query: { ...route.query, tab: 'login' } })
    }
  } catch (error) {
    message.error(error.message || '加载认证配置失败')
  }
}

onMounted(async () => {
  await loadAuthConfig()
  if (needCaptchaForCurrentView()) {
    refreshCaptcha()
  }
})
</script>

<style scoped>
.auth-page {
  --auth-font-display: 'Outfit', 'Noto Sans SC', var(--font-family-sans, sans-serif);
  --auth-font-body: 'Noto Sans SC', 'Outfit', var(--font-family-sans, sans-serif);
  --auth-accent: var(--color-accent, #0891b2);
  --auth-accent-deep: var(--color-accent-deep, #0e7490);
  --auth-accent-light: var(--color-accent-light, #22d3ee);
  --auth-accent-soft: var(--color-accent-soft, rgba(34, 184, 207, 0.12));
  --auth-text-primary: var(--color-text-primary, #0c2a42);
  --auth-text-secondary: var(--color-text-body, #3d6478);
  --auth-text-dim: var(--color-text-secondary, #6a8fa3);
  --auth-text-inverse: var(--color-zone-topbar-text, #f0f9ff);
  --auth-border-strong: var(--color-border-strong, rgba(34, 184, 207, 0.24));
  --auth-input-bg: var(--color-surface-muted, #eef8fc);

  position: fixed;
  inset: 0;
  min-height: 100dvh;
  overflow: auto;
  font-family: var(--auth-font-body);
  color: var(--auth-text-primary);
  background: #b9dce8;
}

.auth-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

/* —— 极光底色 —— */
.auth-aurora {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 62% 54% at 8% 12%, rgba(34, 211, 238, 0.48), transparent 64%),
    radial-gradient(ellipse 44% 50% at 88% 16%, rgba(14, 116, 144, 0.32), transparent 60%),
    radial-gradient(ellipse 50% 46% at 72% 82%, rgba(8, 145, 178, 0.34), transparent 62%),
    radial-gradient(ellipse 36% 58% at 98% 52%, rgba(12, 74, 94, 0.22), transparent 66%),
    radial-gradient(ellipse 28% 30% at 36% 42%, rgba(255, 255, 255, 0.28), transparent 72%),
    radial-gradient(ellipse 22% 24% at 54% 68%, rgba(34, 211, 238, 0.16), transparent 70%),
    linear-gradient(128deg, #9fcfe0 0%, #b8e0ec 28%, #d0edf5 52%, #c0e6f2 76%, #a8d9e8 100%);
  animation: aurora-shift 22s ease-in-out infinite alternate;
}

.auth-aurora-orb {
  position: absolute;
  border-radius: 60% 40% 55% 45%;
  filter: blur(48px);
  opacity: 0.62;
  animation: morph-orb 16s ease-in-out infinite;
  will-change: transform;
}

.auth-aurora-orb--a {
  width: min(46vw, 460px);
  height: min(38vw, 380px);
  top: -10%;
  left: 4%;
  background: rgba(34, 211, 238, 0.4);
}

.auth-aurora-orb--b {
  width: min(40vw, 400px);
  height: min(40vw, 400px);
  right: -8%;
  top: 14%;
  background: rgba(8, 145, 178, 0.32);
  border-radius: 45% 55% 40% 60%;
  animation-duration: 20s;
  animation-delay: -6s;
}

.auth-aurora-orb--c {
  width: min(36vw, 340px);
  height: min(32vw, 300px);
  left: 34%;
  bottom: -8%;
  background: rgba(14, 116, 144, 0.26);
  border-radius: 50% 45% 55% 50%;
  animation-duration: 18s;
  animation-delay: -10s;
}

.auth-desk {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: min(38vh, 320px);
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(148, 196, 214, 0.18) 35%,
    rgba(110, 168, 188, 0.22) 78%,
    rgba(12, 74, 94, 0.1) 100%
  );
  mask-image: linear-gradient(180deg, transparent 0%, #000 48%);
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 48%);
}

/* —— 贝塞尔流线 —— */
.auth-flow {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.95;
}

.auth-flow__path {
  stroke-dasharray: 10 18;
  filter: drop-shadow(0 0 6px rgba(34, 211, 238, 0.25));
  animation: flow-dash 20s linear infinite;
}

.auth-flow__path--b {
  animation-duration: 26s;
  animation-direction: reverse;
}

.auth-flow__path--c {
  stroke-dasharray: 6 16;
  animation-duration: 30s;
}

.auth-flow__node {
  animation: node-pulse 4.5s ease-in-out infinite;
}

.auth-flow__node--b {
  animation-delay: -1.5s;
}

.auth-flow__node--c {
  animation-delay: -2.8s;
}

/* —— 抽象几何：日程环 / 任务卡 —— */
.auth-geo {
  position: absolute;
  inset: 0;
}

.geo-rings {
  position: absolute;
  border-radius: 50%;
  animation: ring-spin 48s linear infinite;
}

.geo-rings span {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1.5px solid rgba(8, 145, 178, 0.2);
}

.geo-rings span:nth-child(2) {
  inset: 14%;
  border-style: dashed;
  border-color: rgba(34, 211, 238, 0.28);
  animation: ring-spin 36s linear infinite reverse;
}

.geo-rings span:nth-child(3) {
  inset: 28%;
  border-color: rgba(14, 116, 144, 0.22);
}

.geo-rings span:nth-child(4) {
  inset: 42%;
  border-style: dotted;
  border-color: rgba(8, 145, 178, 0.32);
}

.geo-rings--a {
  width: min(28vw, 280px);
  height: min(28vw, 280px);
  left: 6%;
  top: 14%;
  opacity: 0.75;
}

.geo-rings--b {
  width: min(18vw, 180px);
  height: min(18vw, 180px);
  right: 18%;
  top: 58%;
  opacity: 0.65;
  animation-duration: 56s;
  animation-direction: reverse;
}

.geo-stack {
  position: absolute;
  width: clamp(120px, 14vw, 168px);
}

.geo-stack--a {
  left: 58%;
  top: 12%;
}

.geo-stack--b {
  left: 8%;
  bottom: 22%;
  width: clamp(90px, 10vw, 120px);
}

.geo-card {
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-top: calc(var(--i, 0) * -12px);
  margin-left: calc(var(--i, 0) * 14px);
  border-radius: 14px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.62), rgba(230, 245, 250, 0.28)),
    rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.62);
  box-shadow:
    0 18px 36px rgba(6, 36, 64, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    inset 0 -1px 0 rgba(8, 145, 178, 0.06);
  backdrop-filter: blur(16px) saturate(150%);
  -webkit-backdrop-filter: blur(16px) saturate(150%);
  transform: rotate(calc(-5deg + var(--i, 0) * 3.5deg));
  animation: card-float 9s ease-in-out infinite;
  animation-delay: calc(var(--i, 0) * -1.8s);
  color: var(--auth-text-primary);
}

.geo-card::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.28);
  pointer-events: none;
}

.geo-card__label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--auth-text-dim);
}

.geo-card__value {
  font-family: var(--auth-font-display);
  font-size: 1.65rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
  color: var(--auth-accent-deep);
}

.geo-card__hint {
  font-size: 11px;
  font-weight: 500;
  color: var(--auth-text-secondary);
}

.geo-card__time {
  font-family: var(--auth-font-display);
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--auth-text-primary);
}

.geo-card__bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 28px;
  margin-top: 4px;
}

.geo-card__bars i {
  display: block;
  width: 6px;
  height: var(--h, 50%);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.85), rgba(8, 145, 178, 0.45));
}

.geo-card--slim {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  background:
    linear-gradient(155deg, rgba(255, 255, 255, 0.5), rgba(34, 211, 238, 0.1)),
    rgba(255, 255, 255, 0.18);
}

.geo-card__slim-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.35);
}

.geo-card__slim-dot--on {
  background: rgba(8, 145, 178, 0.85);
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.45);
}

.geo-card__slim-line {
  display: block;
  height: 5px;
  flex: 1;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(8, 145, 178, 0.4), rgba(34, 211, 238, 0.15));
}

.geo-card__slim-line--short {
  flex: 0.55;
}

.geo-morph {
  position: absolute;
  filter: blur(28px);
  opacity: 0.4;
  animation: morph-orb 14s ease-in-out infinite;
}

.geo-morph--a {
  width: min(22vw, 200px);
  height: min(18vw, 160px);
  left: 42%;
  top: 36%;
  background: rgba(34, 211, 238, 0.28);
  border-radius: 58% 42% 48% 52%;
}

.geo-morph--b {
  width: min(18vw, 160px);
  height: min(18vw, 160px);
  right: 8%;
  bottom: 28%;
  background: rgba(8, 145, 178, 0.2);
  border-radius: 42% 58% 52% 48%;
  animation-delay: -5s;
  animation-duration: 17s;
}

.geo-pulse {
  position: absolute;
  width: 12px;
  height: 12px;
  left: 48%;
  top: 46%;
  border-radius: 50%;
  background: rgba(34, 211, 238, 0.55);
  box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.35);
  animation: geo-pulse 3.6s ease-out infinite;
}

.auth-grain {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

.prop {
  position: absolute;
  opacity: 0.97;
  filter: drop-shadow(0 14px 24px rgba(6, 36, 64, 0.16));
  animation: prop-float 7.5s ease-in-out infinite;
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(440px, 480px);
  gap: clamp(28px, 4vw, 64px);
  align-items: center;
  width: 100%;
  max-width: 1560px;
  min-height: 100dvh;
  margin: 0 auto;
  padding:
    clamp(24px, 3.5vw, 48px)
    clamp(40px, 5.5vw, 88px)
    clamp(40px, 5vw, 68px)
    clamp(36px, 5.5vw, 80px);
}

.auth-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: clamp(20px, 3vw, 32px);
  min-width: 0;
  animation: stage-in 520ms var(--transition-spring, 300ms cubic-bezier(0.16, 1, 0.3, 1)) both;
}

.auth-rail {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: auto;
  padding: clamp(20px, 3vw, 36px) 0 clamp(28px, 4vw, 48px);
  overflow: visible;
  transform: translateX(100px);
}

.auth-rail__glow {
  position: absolute;
  z-index: 0;
  left: 50%;
  top: 42%;
  width: min(140%, 580px);
  height: min(125%, 600px);
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background:
    radial-gradient(
      circle at 50% 42%,
      rgba(214, 238, 246, 0.65) 0%,
      rgba(34, 211, 238, 0.18) 36%,
      rgba(8, 145, 178, 0.12) 56%,
      transparent 74%
    );
  filter: blur(8px);
  pointer-events: none;
}

.auth-rail__props {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.auth-rail .prop--lamp {
  width: clamp(76px, 9vw, 110px);
  right: clamp(-16px, -2vw, 4px);
  top: clamp(-12px, -1vh, 8px);
  animation-delay: -1s;
  animation-duration: 9s;
  z-index: 4;
}

.auth-rail .prop--keyboard {
  width: clamp(140px, 16vw, 190px);
  left: 50%;
  bottom: clamp(-4px, 0vh, 12px);
  transform: translateX(-50%);
  animation: prop-float-keyboard 8.6s ease-in-out infinite;
  animation-delay: -2.2s;
  z-index: 4;
}

.auth-rail .prop--plant {
  width: clamp(52px, 6vw, 72px);
  left: clamp(-20px, -2vw, 0);
  bottom: clamp(88px, 14vh, 140px);
  animation-delay: -4.4s;
  animation-duration: 9.4s;
}

.prop--envelope {
  width: 58px;
  height: 40px;
  right: clamp(-4px, 0vw, 16px);
  bottom: clamp(100px, 16vh, 160px);
  border-radius: 4px;
  background: linear-gradient(155deg, #fff, #e8f4f8);
  border: 1px solid rgba(12, 74, 94, 0.28);
  box-shadow: 0 12px 22px rgba(6, 36, 64, 0.12);
  transform: rotate(14deg);
  filter: none;
  animation: prop-float-envelope 9s ease-in-out infinite;
}

.prop--envelope::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 48%, rgba(8, 145, 178, 0.2) 50%, transparent 52%),
    linear-gradient(-135deg, transparent 48%, rgba(8, 145, 178, 0.18) 50%, transparent 52%);
}

.prop--coaster {
  width: 36px;
  height: 36px;
  left: clamp(-8px, 0vw, 12px);
  top: clamp(48px, 8vh, 80px);
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(255, 248, 230, 0.95), rgba(8, 145, 178, 0.2));
  border: 2px solid rgba(12, 74, 94, 0.22);
  box-shadow: 0 8px 16px rgba(6, 36, 64, 0.1);
  filter: none;
  animation-delay: -2.8s;
  animation-duration: 11s;
}

.stage-floaters {
  position: absolute;
  inset: -12px -8px -20px -8px;
  z-index: 5;
  pointer-events: none;
}

.stage-floaters .prop {
  z-index: 3;
}

.prop--sticky {
  display: flex;
  flex-direction: column;
  gap: 4px;
  left: auto;
  right: 4%;
  top: 6%;
  width: 72px;
  padding: 10px 10px 12px;
  border-radius: 3px 3px 4px 4px;
  font-family: var(--auth-font-display);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  line-height: 1.25;
  color: #0c4a5c;
  background: linear-gradient(160deg, #dff4f8, #c8ebf2);
  box-shadow:
    0 8px 20px rgba(6, 36, 64, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
  animation: prop-float-sticky 6.8s ease-in-out infinite;
  animation-delay: -1.2s;
}

.prop--sticky span:last-child {
  font-weight: 500;
  color: #3d6478;
}

.prop--sticky-b {
  left: -4%;
  right: auto;
  top: auto;
  bottom: 18%;
  width: 68px;
  background: linear-gradient(160deg, #fff4d8, #ffe8b0);
  color: #6b4e1e;
  animation: prop-float-sticky-b 7.4s ease-in-out infinite;
  animation-delay: -3.6s;
}

.prop--sticky-b span:last-child {
  color: #8a6a2e;
}

.prop--chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  font-family: var(--auth-font-display);
  color: #0c4a5c;
  background: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.82),
    rgba(230, 245, 250, 0.68)
  );
  border: 1px solid rgba(34, 184, 207, 0.28);
  box-shadow:
    0 12px 28px rgba(6, 36, 64, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
}

.prop--chip-progress {
  left: auto;
  right: -2%;
  top: 38%;
  width: 118px;
  animation-delay: -3.1s;
  animation-duration: 8.4s;
}

.prop-chip__label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #3d6478;
}

.prop-chip__bars {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  height: 36px;
}

.prop-chip__bars i {
  display: block;
  flex: 1;
  height: var(--h, 50%);
  border-radius: 3px 3px 1px 1px;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.75), rgba(8, 145, 178, 0.35));
}

.prop--chip-clock {
  align-items: flex-start;
  left: auto;
  right: 6%;
  bottom: 2%;
  top: auto;
  width: 108px;
  animation-delay: -5.2s;
  animation-duration: 7.8s;
}

.prop-chip__ring {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(8, 145, 178, 0.22);
  background:
    conic-gradient(from 210deg, rgba(8, 145, 178, 0.7) 0 68%, rgba(8, 145, 178, 0.12) 68% 100%);
  mask: radial-gradient(circle at center, transparent 54%, #000 56%);
  -webkit-mask: radial-gradient(circle at center, transparent 54%, #000 56%);
}

.prop-chip__time {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.prop-chip__hint {
  margin-top: -2px;
  font-size: 11px;
  font-weight: 500;
  color: #3d6478;
}

.prop--chip-todo {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  left: -2%;
  right: auto;
  top: 18%;
  width: auto;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 600;
  animation-delay: -2s;
  animation-duration: 9.2s;
}

.prop--chip-mail {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  left: auto;
  right: 18%;
  top: -4%;
  width: auto;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  animation-delay: -4.8s;
  animation-duration: 8s;
}

.prop-chip__mail-icon {
  width: 16px;
  height: 12px;
  border-radius: 2px;
  border: 1.5px solid rgba(8, 145, 178, 0.55);
  background: rgba(255, 255, 255, 0.7);
  position: relative;
  flex-shrink: 0;
}

.prop-chip__mail-icon::before {
  content: '';
  position: absolute;
  left: 1px;
  right: 1px;
  top: 1px;
  height: 7px;
  border-left: 1.5px solid rgba(8, 145, 178, 0.45);
  border-right: 1.5px solid rgba(8, 145, 178, 0.45);
  border-top: 1.5px solid rgba(8, 145, 178, 0.45);
  transform: perspective(8px) rotateX(28deg);
  transform-origin: top;
}

.prop-chip__check {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  background: rgba(8, 145, 178, 0.85);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  position: relative;
  flex-shrink: 0;
}

.prop-chip__check::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 2px;
  width: 4px;
  height: 7px;
  border: solid #fff;
  border-width: 0 1.5px 1.5px 0;
  transform: rotate(40deg);
}

.prop--chip-focus {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  left: 8%;
  right: auto;
  top: auto;
  bottom: 28%;
  width: auto;
  padding: 9px 12px;
  font-size: 12px;
  font-weight: 650;
  animation-delay: -1.4s;
  animation-duration: 8.6s;
}

.prop-chip__pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: rgba(8, 145, 178, 0.9);
  box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.5);
  animation: focus-pulse 2.2s ease-out infinite;
}

.prop--chip-sync {
  flex-direction: row;
  align-items: center;
  gap: 7px;
  left: auto;
  right: 2%;
  top: 22%;
  width: auto;
  padding: 8px 11px;
  font-size: 11px;
  font-weight: 600;
  color: var(--auth-accent-deep);
  animation-delay: -6s;
  animation-duration: 9s;
}

.prop-chip__cloud {
  width: 16px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 8px 8px 4px 4px;
  background: rgba(8, 145, 178, 0.35);
  position: relative;
}

.prop-chip__cloud::before {
  content: '';
  position: absolute;
  left: 2px;
  top: -4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(8, 145, 178, 0.35);
  box-shadow: 6px 1px 0 rgba(34, 211, 238, 0.45);
}

.prop--chip-ai {
  flex-direction: row;
  align-items: center;
  gap: 7px;
  left: auto;
  right: -4%;
  bottom: 36%;
  top: auto;
  width: auto;
  padding: 8px 12px;
  font-size: 11px;
  font-weight: 650;
  background: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.88),
    rgba(34, 211, 238, 0.18)
  );
  animation-delay: -2.6s;
  animation-duration: 8.2s;
}

.prop-chip__spark {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  background: var(--auth-accent);
  clip-path: polygon(50% 0%, 62% 38%, 100% 50%, 62% 62%, 50% 100%, 38% 62%, 0% 50%, 38% 38%);
}

@keyframes focus-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.45);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(34, 211, 238, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0);
  }
}

.stage-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--auth-accent, #0891b2);
  animation: stage-in 560ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.stage-brand__mark {
  display: inline-flex;
  flex-shrink: 0;
  filter: drop-shadow(0 6px 16px rgba(8, 145, 178, 0.28));
}

.stage-brand__en {
  margin: 0;
  font-family: var(--auth-font-display);
  font-size: clamp(1.85rem, 2.9vw, 2.45rem);
  font-weight: 800;
  letter-spacing: -0.055em;
  line-height: 1.05;
  color: var(--auth-text-primary);
}

.stage-brand__zh {
  margin: 6px 0 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.28em;
  color: var(--auth-accent-deep);
}

.stage-title {
  margin: 22px 0 0;
  max-width: 8.5em;
  font-family: var(--auth-font-display);
  font-size: clamp(2.05rem, 3.8vw, 3rem);
  font-weight: 700;
  letter-spacing: -0.05em;
  line-height: 1.1;
  color: var(--auth-text-primary);
  text-wrap: balance;
  animation: stage-in 640ms cubic-bezier(0.16, 1, 0.3, 1) 80ms both;
}

/* —— 办公桌 + 笔记本场景 —— */
.stage-mock {
  position: relative;
  margin-top: 4px;
  overflow: visible;
}

.desk-scene {
  position: relative;
  min-height: clamp(280px, 42vw, 420px);
  padding: 12px 10px 36px;
  perspective: 1400px;
}

.desk-photo {
  position: absolute;
  left: -6%;
  right: -10%;
  bottom: -4%;
  height: 58%;
  background:
    linear-gradient(
      180deg,
      rgba(185, 220, 232, 0.55) 0%,
      rgba(185, 220, 232, 0.12) 38%,
      transparent 70%
    ),
    url('/auth-desk-scene.png') center 38% / cover no-repeat;
  opacity: 0.42;
  filter: saturate(0.85) contrast(1.05);
  mask-image: linear-gradient(180deg, transparent 0%, #000 28%, #000 78%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, transparent 0%, #000 28%, #000 78%, transparent 100%);
  pointer-events: none;
  z-index: 0;
}

.desk-surface {
  position: absolute;
  left: -4%;
  right: -8%;
  bottom: 2%;
  height: 46%;
  z-index: 1;
  transform: rotateX(62deg) scaleY(0.72);
  transform-origin: center bottom;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(214, 234, 242, 0.55) 0%, rgba(148, 188, 206, 0.72) 55%, rgba(110, 160, 180, 0.85) 100%);
  box-shadow:
    0 24px 48px rgba(6, 36, 64, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.45);
  border: 1px solid rgba(12, 74, 94, 0.18);
}

.desk-surface__shine {
  position: absolute;
  inset: 8% 12% auto;
  height: 36%;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.35), transparent 70%);
  filter: blur(8px);
}

.desk-surface__edge {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -10px;
  height: 14px;
  border-radius: 0 0 12px 12px;
  background: linear-gradient(180deg, rgba(12, 74, 94, 0.22), rgba(12, 74, 94, 0.38));
  transform: rotateX(-8deg);
  transform-origin: top center;
}

.desk-devices {
  position: relative;
  z-index: 3;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: clamp(12px, 2vw, 20px);
  align-items: end;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 2% 6%;
}

.mock-monitor {
  position: relative;
  min-width: 0;
  transform: rotateY(-3deg) rotateX(2deg);
  transform-style: preserve-3d;
  animation: device-settle 700ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.mock-monitor__frame {
  position: relative;
  padding: 14px 14px 12px;
  border-radius: 14px;
  background: linear-gradient(
    165deg,
    rgba(88, 118, 132, 0.98) 0%,
    rgba(62, 92, 108, 0.98) 48%,
    rgba(48, 76, 92, 0.97) 100%
  );
  border: 1.5px solid rgba(12, 42, 58, 0.55);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    inset 0 -2px 0 rgba(0, 0, 0, 0.18),
    0 18px 36px rgba(6, 36, 64, 0.22);
}

.mock-monitor__frame::before {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 10px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.mock-monitor__cam {
  display: block;
  width: 6px;
  height: 6px;
  margin: 0 auto 8px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #9ad8e6, #0a3344 72%);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.12);
}

.mock-monitor__chin {
  height: 10px;
  margin: 0 8%;
  border-radius: 0 0 6px 6px;
  background: linear-gradient(
    180deg,
    rgba(72, 100, 114, 0.95),
    rgba(52, 78, 92, 0.98)
  );
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.mock-monitor__neck {
  width: 14px;
  height: clamp(22px, 3vw, 32px);
  margin: 0 auto;
  background: linear-gradient(
    90deg,
    rgba(70, 98, 112, 0.95),
    rgba(140, 170, 184, 0.75) 45%,
    rgba(60, 88, 102, 0.95)
  );
  border-radius: 2px;
  box-shadow: 0 4px 8px rgba(6, 36, 64, 0.12);
}

.mock-monitor__foot {
  width: min(42%, 160px);
  height: 8px;
  margin: 0 auto;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    rgba(90, 120, 134, 0.95),
    rgba(48, 76, 90, 0.98)
  );
  box-shadow:
    0 6px 14px rgba(6, 36, 64, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.mock-monitor__shadow {
  position: absolute;
  left: 12%;
  right: 12%;
  bottom: -14px;
  height: 18px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(6, 36, 64, 0.3), transparent 72%);
  filter: blur(5px);
  z-index: -1;
  pointer-events: none;
}

.mock-tablet {
  position: relative;
  width: clamp(92px, 12vw, 128px);
  flex-shrink: 0;
  margin-bottom: clamp(18px, 3vw, 36px);
  transform: rotateZ(8deg) rotateY(-8deg);
  animation: device-settle 780ms cubic-bezier(0.16, 1, 0.3, 1) 90ms both;
}

.mock-tablet__bezel {
  padding: 8px;
  border-radius: 14px;
  background: linear-gradient(
    160deg,
    rgba(214, 232, 240, 0.98),
    rgba(156, 192, 208, 0.96)
  );
  border: 1.5px solid rgba(12, 74, 94, 0.4);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.75),
    0 14px 28px rgba(6, 36, 64, 0.18);
}

.mock-tablet__screen {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 148px;
  padding: 12px 10px;
  border-radius: 8px;
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.94),
    rgba(232, 245, 250, 0.88)
  );
  border: 1px solid rgba(34, 184, 207, 0.2);
}

.mock-tablet__bar {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 11px;
  color: var(--auth-text-dim);
}

.mock-tablet__bar strong {
  font-family: var(--auth-font-display);
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: var(--auth-accent-deep);
}

.mock-tablet__list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.mock-tablet__list i {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(8, 145, 178, 0.45), rgba(34, 211, 238, 0.18));
}

.mock-tablet__list i:nth-child(2) {
  width: 78%;
}

.mock-tablet__list i:nth-child(3) {
  width: 58%;
  opacity: 0.7;
}

.mock-tablet__chip {
  margin-top: auto;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--auth-accent-deep);
  background: rgba(34, 211, 238, 0.16);
  border: 1px solid rgba(8, 145, 178, 0.2);
}

.mock-tablet__shadow {
  position: absolute;
  left: 8%;
  right: -4%;
  bottom: -10px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(6, 36, 64, 0.26), transparent 70%);
  filter: blur(4px);
  z-index: -1;
}

@keyframes device-settle {
  from {
    opacity: 0;
    translate: 0 18px;
  }
  to {
    opacity: 1;
    translate: 0 0;
  }
}

.desk-props {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
}

.desk-prop {
  position: absolute;
  filter: drop-shadow(0 10px 16px rgba(6, 36, 64, 0.18));
  animation: prop-float 8s ease-in-out infinite;
}

.desk-prop--coffee {
  width: clamp(56px, 7vw, 84px);
  left: 0;
  bottom: 8%;
  animation-delay: -1.2s;
}

.desk-prop--notebook {
  width: clamp(72px, 8.5vw, 104px);
  right: -2%;
  bottom: 4%;
  animation-delay: -2.8s;
  animation-duration: 9.2s;
}

.desk-prop--pen {
  width: clamp(16px, 2vw, 22px);
  right: 18%;
  bottom: 22%;
  transform: rotate(32deg);
  animation: prop-float-pen 8.4s ease-in-out infinite;
  animation-delay: -3.6s;
}

.desk-prop--mouse {
  width: clamp(36px, 4.2vw, 48px);
  left: 18%;
  bottom: 6%;
  animation-delay: -5s;
  animation-duration: 8.6s;
}

.desk-prop--plant {
  width: clamp(48px, 5.5vw, 68px);
  left: 2%;
  top: 12%;
  animation-delay: -4.2s;
  animation-duration: 9.5s;
}

.desk-prop--phone {
  width: clamp(44px, 5vw, 58px);
  left: 22%;
  bottom: 4%;
  animation-delay: -0.8s;
  animation-duration: 8.8s;
}

.desk-phone__pad {
  position: absolute;
  left: 50%;
  bottom: -2px;
  width: 120%;
  height: 14px;
  border-radius: 999px;
  transform: translateX(-50%);
  background: rgba(70, 98, 112, 0.55);
  box-shadow:
    0 4px 10px rgba(6, 36, 64, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.desk-phone__glow {
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 90%;
  height: 18px;
  border-radius: 50%;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, rgba(34, 211, 238, 0.45), transparent 70%);
  filter: blur(4px);
  animation: charge-glow 2.8s ease-in-out infinite;
}

.desk-phone__device {
  position: relative;
  z-index: 1;
  width: 100%;
  display: block;
}

.desk-prop--buds {
  width: clamp(48px, 5.5vw, 64px);
  left: 34%;
  bottom: 18%;
  animation-delay: -3.2s;
  animation-duration: 9.4s;
}

.desk-prop--dock {
  width: clamp(72px, 8vw, 96px);
  left: 48%;
  bottom: 2%;
  animation-delay: -5.5s;
  animation-duration: 10s;
}

.desk-prop--speaker {
  width: clamp(40px, 4.8vw, 54px);
  right: 22%;
  bottom: 26%;
  animation-delay: -1.8s;
  animation-duration: 8.5s;
}

.desk-prop--trackpad {
  width: clamp(56px, 6.5vw, 76px);
  left: 8%;
  bottom: 2%;
  animation-delay: -4.6s;
  animation-duration: 9.1s;
}

@keyframes charge-glow {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}

.mock-plane {
  display: grid;
  grid-template-columns: 1.05fr 1.2fr;
  grid-template-rows: auto auto;
  gap: 12px;
  padding: 14px;
  border-radius: 8px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.88), rgba(236, 246, 252, 0.72));
  border: 1px solid rgba(34, 184, 207, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
}

.mock-pane {
  padding: 14px 14px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(34, 184, 207, 0.18);
  box-shadow: 0 6px 16px rgba(6, 36, 64, 0.05);
  animation: mock-rise 560ms var(--transition-spring, 300ms cubic-bezier(0.16, 1, 0.3, 1)) both;
  animation-delay: calc(120ms + var(--i, 0) * 90ms);
}

.mock-pane--todo {
  grid-column: 1;
  grid-row: 1 / span 2;
}

.mock-pane--cal {
  grid-column: 2;
  grid-row: 1;
}

.mock-pane--board {
  grid-column: 2;
  grid-row: 2;
}

.mock-pane__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.mock-pane__label {
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0.02em;
  color: var(--auth-text-primary, #0c2a42);
}

.mock-pane__meta {
  font-size: 11px;
  color: var(--auth-text-dim, #6a8fa3);
}

.mock-todo {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mock-todo__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 28px;
}

.mock-check {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  border-radius: 5px;
  border: 1.5px solid rgba(8, 145, 178, 0.45);
  background: #fff;
  position: relative;
}

.mock-todo__item.is-done .mock-check {
  background: var(--auth-accent, #0891b2);
  border-color: var(--auth-accent, #0891b2);
}

.mock-todo__item.is-done .mock-check::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 5px;
  height: 9px;
  border: solid #fff;
  border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg);
}

.mock-todo__text {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--auth-text-secondary, #3d6478);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mock-todo__item.is-done .mock-todo__text {
  color: var(--auth-text-dim, #6a8fa3);
  text-decoration: line-through;
  text-decoration-color: rgba(106, 143, 163, 0.55);
}

.mock-prio {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mock-prio--high {
  background: var(--color-red, #e05545);
}

.mock-prio--mid {
  background: var(--color-orange, #e07a4f);
}

.mock-week {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.mock-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px 2px 8px;
  border-radius: 10px;
  background: rgba(240, 247, 251, 0.85);
}

.mock-day.is-today {
  background: rgba(8, 145, 178, 0.12);
  box-shadow: inset 0 0 0 1px rgba(8, 145, 178, 0.28);
}

.mock-day__label {
  font-size: 10px;
  color: var(--auth-text-dim, #6a8fa3);
}

.mock-day__num {
  font-family: var(--auth-font-display);
  font-size: 13px;
  font-weight: 650;
  color: var(--auth-text-primary, #0c2a42);
}

.mock-block {
  width: 70%;
  height: 5px;
  border-radius: 999px;
  margin-top: 2px;
}

.mock-block.tone-a {
  background: var(--auth-accent, #0891b2);
}

.mock-block.tone-b {
  background: var(--color-purple, #8b7ec8);
}

.mock-block.tone-c {
  background: var(--color-orange, #e07a4f);
}

.mock-event {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  background: linear-gradient(90deg, rgba(8, 145, 178, 0.12), rgba(34, 211, 238, 0.06));
  border: 1px solid rgba(8, 145, 178, 0.2);
}

.mock-event--pulse {
  position: relative;
  overflow: hidden;
}

.mock-event--pulse::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    transparent 35%,
    rgba(255, 255, 255, 0.45) 50%,
    transparent 65%
  );
  transform: translateX(-120%);
  animation: event-sheen 3.6s ease-in-out 1.2s infinite;
}

.mock-event__time {
  font-family: var(--auth-font-display);
  font-size: 12px;
  font-weight: 650;
  color: var(--auth-accent-deep, #0e7490);
}

.mock-event__title {
  font-size: 12px;
  color: var(--auth-text-secondary, #3d6478);
}

.mock-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.mock-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.mock-col__name {
  font-size: 10px;
  font-weight: 600;
  color: var(--auth-text-dim, #6a8fa3);
  letter-spacing: 0.02em;
}

.mock-card {
  height: 22px;
  border-radius: 6px;
  background: linear-gradient(180deg, #f4fafc, #e7f3f7);
  border: 1px solid rgba(34, 184, 207, 0.16);
}

.mock-col:nth-child(2) .mock-card {
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.18), rgba(8, 145, 178, 0.08));
  border-color: rgba(8, 145, 178, 0.22);
}

.mock-col:nth-child(3) .mock-card {
  background: linear-gradient(180deg, rgba(74, 186, 106, 0.14), rgba(74, 186, 106, 0.05));
  border-color: rgba(74, 186, 106, 0.22);
}

/* —— 右侧表单（web glass approx） —— */
.form-card {
  position: relative;
  isolation: isolate;
  z-index: 3;
  width: 100%;
  max-width: 480px;
  margin: 0;
  padding: 42px 38px 32px;
  overflow: hidden;
  background:
    linear-gradient(155deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.52)),
    rgba(240, 249, 252, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  box-shadow:
    0 28px 56px rgba(6, 36, 64, 0.16),
    0 10px 28px rgba(8, 145, 178, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    inset 0 -1px 0 rgba(8, 145, 178, 0.06);
  backdrop-filter: blur(22px) saturate(160%) contrast(1.02);
  -webkit-backdrop-filter: blur(22px) saturate(160%) contrast(1.02);
  animation: card-in 520ms cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 100ms;
}

.form-card::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  border-radius: inherit;
  background:
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.55), transparent 42%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.2), transparent 40%, rgba(34, 211, 238, 0.06));
  pointer-events: none;
}

.form-card::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: calc(16px - 1px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  pointer-events: none;
}

.auth-panel {
  width: 100%;
}

.auth-slide-up-enter-active,
.auth-slide-up-leave-active {
  transition:
    opacity 220ms ease,
    transform 220ms var(--transition-spring, 300ms cubic-bezier(0.16, 1, 0.3, 1));
}

.auth-slide-up-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.auth-slide-up-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes stage-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes mock-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes event-sheen {
  0%,
  55% {
    transform: translateX(-120%);
  }
  75%,
  100% {
    transform: translateX(120%);
  }
}

@keyframes prop-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes prop-float-pen {
  0%,
  100% {
    transform: rotate(28deg) translateY(0);
  }
  50% {
    transform: rotate(28deg) translateY(-7px);
  }
}

@keyframes prop-float-sticky {
  0%,
  100% {
    transform: rotate(6deg) translateY(0);
  }
  50% {
    transform: rotate(6deg) translateY(-6px);
  }
}

@keyframes prop-float-sticky-b {
  0%,
  100% {
    transform: rotate(-8deg) translateY(0);
  }
  50% {
    transform: rotate(-8deg) translateY(-7px);
  }
}

@keyframes prop-float-keyboard {
  0%,
  100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(-7px);
  }
}

@keyframes flow-dash {
  to {
    stroke-dashoffset: -140;
  }
}

@keyframes aurora-shift {
  from {
    filter: hue-rotate(-6deg) saturate(1);
  }
  to {
    filter: hue-rotate(8deg) saturate(1.08);
  }
}

@keyframes morph-orb {
  0%,
  100% {
    border-radius: 60% 40% 55% 45%;
    transform: translate(0, 0) scale(1);
  }
  33% {
    border-radius: 45% 55% 42% 58%;
    transform: translate(2%, -3%) scale(1.04);
  }
  66% {
    border-radius: 52% 48% 60% 40%;
    transform: translate(-2%, 2%) scale(0.97);
  }
}

@keyframes ring-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes card-float {
  0%,
  100% {
    transform: rotate(calc(-4deg + var(--i, 0) * 3deg)) translateY(0);
  }
  50% {
    transform: rotate(calc(-4deg + var(--i, 0) * 3deg)) translateY(-8px);
  }
}

@keyframes node-pulse {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.25);
  }
}

@keyframes geo-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.4);
    opacity: 0.8;
  }
  70% {
    box-shadow: 0 0 0 28px rgba(34, 211, 238, 0);
    opacity: 0;
  }
  100% {
    opacity: 0;
  }
}

@media (prefers-reduced-transparency: reduce) {
  .form-card,
  .geo-card {
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    background: rgba(255, 255, 255, 0.96);
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-stage,
  .stage-brand,
  .stage-title,
  .form-card,
  .mock-pane,
  .auth-aurora,
  .auth-aurora-orb,
  .auth-flow__path,
  .auth-flow__node,
  .geo-rings,
  .geo-rings span,
  .geo-card,
  .geo-morph,
  .geo-pulse,
  .prop,
  .desk-prop,
  .mock-monitor,
  .mock-tablet {
    animation: none;
  }

  .desk-prop--pen {
    transform: rotate(32deg);
  }

  .prop--sticky {
    transform: rotate(6deg);
  }

  .prop--sticky-b {
    transform: rotate(-8deg);
  }

  .mock-monitor {
    transform: rotateY(-3deg) rotateX(2deg);
  }

  .mock-tablet {
    transform: rotateZ(8deg) rotateY(-8deg);
  }

  .auth-rail .prop--keyboard {
    transform: translateX(-50%);
  }

  .prop-chip__pulse {
    animation: none;
  }

  .desk-phone__glow {
    animation: none;
    opacity: 0.65;
  }

  .mock-event--pulse::after {
    animation: none;
    display: none;
  }

  .auth-slide-up-enter-active,
  .auth-slide-up-leave-active {
    transition: none;
  }

  .auth-slide-up-enter-from,
  .auth-slide-up-leave-to {
    transform: none;
  }
}

.auth-head {
  margin-bottom: 24px;
}

.auth-title {
  margin: 0;
  font-family: var(--auth-font-display);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
  color: var(--auth-text-primary, #0c2a42);
  text-wrap: balance;
}

.auth-sub {
  margin: 8px 0 0;
  max-width: 22em;
  font-size: 13px;
  line-height: 1.45;
  color: var(--auth-text-dim);
  text-wrap: pretty;
}

.auth-form {
  margin: 0;
}

.form-card :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.form-card :deep(.ant-form-item.form-item-last) {
  margin-bottom: 0;
}

.form-card :deep(.ant-form-item-label) {
  padding: 0 0 6px !important;
}

.form-card :deep(.ant-form-item-label > label) {
  height: auto;
}

.label-text {
  font-size: 13px;
  font-weight: 550;
  color: var(--auth-text-primary, #0c2a42);
}

.captcha-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
  width: 100%;
}

.captcha-row :deep(.ant-input-affix-wrapper) {
  flex: 1;
  min-width: 0;
}

.captcha-box {
  width: 108px;
  min-height: 40px;
  border-radius: var(--radius-control, 10px);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--auth-border-strong, rgba(34, 184, 207, 0.4));
  background: var(--auth-input-bg, rgba(255, 255, 255, 0.78));
  flex-shrink: 0;
  padding: 0;
  transition:
    border-color var(--transition-fast, 150ms ease),
    box-shadow var(--transition-fast, 150ms ease);
}

.captcha-box:hover {
  border-color: var(--auth-accent, #0891b2);
  box-shadow: 0 0 0 3px var(--auth-accent-soft, rgba(34, 184, 207, 0.16));
}

.captcha-box:focus-visible {
  outline: 2px solid var(--auth-accent, #0891b2);
  outline-offset: 2px;
}

.captcha-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: var(--auth-text-dim, #6a8fa3);
}

.auth-foot {
  margin-top: 20px;
}

.option-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 14px;
}

.check-label {
  color: var(--auth-text-secondary, #3d6478);
}

.link {
  font-size: 14px;
  color: var(--auth-accent, #0891b2);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  transition: color var(--transition-fast, 150ms ease);
}

.link:hover {
  color: var(--auth-accent-deep, #0e7490);
}

.link:focus-visible {
  outline: 2px solid var(--auth-accent, #0891b2);
  outline-offset: 3px;
  border-radius: 2px;
}

.link--muted {
  color: var(--auth-text-dim, #6a8fa3);
  cursor: default;
}

.link--muted:hover {
  color: var(--auth-text-dim, #6a8fa3);
}

.form-card :deep(.ant-input-affix-wrapper),
.form-card :deep(.ant-input) {
  background: var(--auth-input-bg, rgba(255, 255, 255, 0.78));
  border: 1px solid var(--auth-border-strong, rgba(34, 184, 207, 0.4));
  box-shadow: none;
  border-radius: var(--radius-control, 10px);
  transition:
    border-color var(--transition-fast, 150ms ease),
    box-shadow var(--transition-fast, 150ms ease);
}

.form-card :deep(.ant-input-affix-wrapper .ant-input),
.form-card :deep(.ant-input-password .ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  border-radius: 0;
}

.form-card :deep(.ant-input-affix-wrapper-focused),
.form-card :deep(.ant-input-affix-wrapper:focus-within),
.form-card :deep(.ant-input:focus) {
  border-color: var(--auth-accent, #0891b2);
  box-shadow: 0 0 0 3px var(--auth-accent-soft, rgba(34, 184, 207, 0.16));
}

.form-card :deep(.ant-input-affix-wrapper .ant-input:focus),
.form-card :deep(.ant-input-password .ant-input:focus) {
  border: none !important;
  box-shadow: none !important;
}

.form-card :deep(.ant-btn.submit-btn) {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: var(--radius-control, 10px);
  font-weight: 650;
  letter-spacing: 0.03em;
  color: var(--auth-text-inverse);
  background: linear-gradient(
    135deg,
    var(--auth-accent-light),
    var(--auth-accent) 48%,
    var(--auth-accent-deep)
  );
  box-shadow: 0 6px 18px rgba(8, 145, 178, 0.38);
  transition:
    transform var(--transition-fast, 150ms ease),
    box-shadow var(--transition-spring, 300ms cubic-bezier(0.16, 1, 0.3, 1)),
    background var(--transition-fast, 150ms ease);
}

.form-card :deep(.ant-btn.submit-btn:hover) {
  color: var(--auth-text-inverse);
  background: linear-gradient(
    135deg,
    var(--auth-accent),
    var(--auth-accent-deep)
  ) !important;
  transform: translateY(-1px);
  box-shadow: 0 10px 26px rgba(8, 145, 178, 0.45);
}

.form-card :deep(.ant-btn.submit-btn:active) {
  transform: scale(0.98);
}

.form-card :deep(.ant-btn.submit-btn:focus-visible) {
  outline: 2px solid var(--auth-accent, #0891b2);
  outline-offset: 3px;
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-arrow {
  transition: transform var(--transition-fast, 150ms ease);
}

.form-card :deep(.ant-btn.submit-btn:hover) .btn-arrow {
  transform: translateX(2px);
}

.switch-line {
  margin: 16px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--auth-text-dim, #6a8fa3);
}

.switch-line .link {
  font-size: 13px;
  font-weight: 550;
}

@media (max-width: 1180px) {
  .auth-shell {
    grid-template-columns: minmax(0, 1fr) minmax(400px, 440px);
  }

  .prop--chip-progress,
  .prop--chip-clock {
    right: 0;
  }

  .desk-prop--plant,
  .desk-prop--mouse,
  .desk-prop--speaker,
  .desk-prop--trackpad,
  .prop--chip-ai {
    display: none;
  }

  .desk-photo {
    opacity: 0.32;
  }

  .geo-stack--b,
  .geo-rings--b {
    display: none;
  }

  .auth-rail .prop--keyboard {
    width: clamp(120px, 15vw, 160px);
  }
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 28px 24px 40px;
  }

  .auth-stage {
    order: 1;
    padding: 0;
  }

  .auth-rail {
    order: 2;
    padding: 16px 0 20px;
    justify-content: center;
    transform: none;
  }

  .auth-rail .prop--lamp {
    top: -8px;
    right: 8px;
    width: 68px;
  }

  .auth-rail .prop--plant {
    left: 4px;
    bottom: 64px;
  }

  .auth-rail .prop--keyboard {
    bottom: -4px;
    width: 150px;
  }

  .form-card {
    max-width: 480px;
  }

  .auth-desk {
    height: 26vh;
  }

  .desk-prop--pen,
  .desk-prop--notebook,
  .desk-prop--plant,
  .desk-prop--mouse,
  .desk-prop--speaker,
  .desk-prop--dock,
  .desk-prop--buds,
  .desk-prop--trackpad,
  .prop--chip-todo,
  .prop--chip-progress,
  .prop--chip-mail,
  .prop--chip-ai,
  .prop--chip-sync,
  .prop--sticky-b,
  .prop--envelope,
  .auth-geo,
  .auth-flow {
    display: none;
  }

  .desk-photo {
    opacity: 0.28;
    height: 48%;
  }

  .desk-surface {
    height: 38%;
  }

  .prop--sticky {
    right: 8px;
    top: 0;
  }

  .prop--chip-clock {
    right: 8px;
    bottom: -4px;
  }

  .mock-monitor {
    transform: none;
  }

  .mock-tablet {
    display: none;
  }

  .mock-monitor__shadow {
    bottom: -10px;
  }

  .stage-title {
    max-width: none;
  }

  .mock-pane--todo {
    grid-column: 1 / -1;
    grid-row: auto;
  }

  .mock-pane--cal,
  .mock-pane--board {
    grid-column: auto;
    grid-row: auto;
  }

  .mock-plane {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .stage-brand__en {
    font-size: 1.45rem;
  }

  .stage-title {
    font-size: 1.6rem;
  }

  .stage-floaters,
  .desk-props {
    display: none;
  }

  .auth-rail__props .prop--lamp,
  .auth-rail__props .prop--plant,
  .auth-rail__props .prop--envelope,
  .auth-rail__props .prop--coaster {
    display: none;
  }

  .desk-photo {
    opacity: 0.22;
  }

  .desk-surface {
    height: 32%;
    opacity: 0.85;
  }

  .auth-desk {
    height: 18vh;
    opacity: 0.65;
  }

  .mock-pane--todo,
  .mock-pane--board {
    display: none;
  }

  .mock-plane {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .mock-pane--cal {
    grid-column: 1;
  }

  .mock-monitor__frame {
    padding: 10px 10px 8px;
  }

  .mock-monitor__cam {
    margin-bottom: 6px;
  }

  .desk-devices {
    grid-template-columns: 1fr;
    padding-bottom: 4%;
  }
}

@media (max-width: 480px) {
  .auth-shell {
    padding: 20px 16px 32px;
  }

  .auth-rail {
    padding: 4px 0 8px;
  }

  .form-card {
    padding: 28px 20px 22px;
    border-radius: var(--radius-lg, 12px);
  }

  .auth-title {
    font-size: 24px;
  }

  .captcha-box {
    width: 96px;
  }

  .stage-brand__zh {
    letter-spacing: 0.12em;
  }

  .auth-rail .prop--keyboard {
    display: none;
  }
}
</style>
