<template>
  <div class="auth-page">
    <section class="auth-visual" aria-label="产品介绍">
      <div
        class="auth-visual__img"
        aria-hidden="true"
        style="background-image: url('/AuthBackground.png?v=2')"
      />
      <header class="stage-brand">
        <span class="stage-brand__mark">
          <BrandMarkView :size="32" />
        </span>
        <div class="stage-brand__text">
          <p class="stage-brand__en">Personal Workbench</p>
          <p class="stage-brand__zh">个人工作台</p>
        </div>
      </header>
      <div class="stage-intro">
        <h1 class="stage-title">今天要推进的工作，都在这里</h1>
        <p class="stage-lead">你可以把今天要做的事，收进同一张桌面。</p>
        <ul class="stage-features" aria-label="核心功能">
          <li v-for="item in coreFeatures" :key="item.title" class="stage-feature">
            <span class="stage-feature__dot" aria-hidden="true" />
            <div class="stage-feature__body">
              <p class="stage-feature__title">{{ item.title }}</p>
              <p class="stage-feature__desc">{{ item.desc }}</p>
            </div>
          </li>
        </ul>
      </div>
    </section>

    <aside class="auth-form-zone" aria-label="登录区域">
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
</template>

<script setup>
/** 登录/注册页：左插画 + 右同色纯底表单区 */
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

const coreFeatures = [
  { title: '事务看板', desc: '任务与项目同屏推进，拖拽排序跟手' },
  { title: '日程安排', desc: '今天的时间一眼可见，可关联任务' },
  { title: 'AI 助手', desc: '流式对话与项目智能规划，一键落板' },
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

function completeLogin(data, successMsg = '登录成功') {
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
  message.success(successMsg)
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
    completeLogin(data, `注册成功，已自动登录（账号：${data.account}）`)
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
  --auth-font-brand: 'Outfit', 'Noto Sans SC', var(--font-family-sans, sans-serif);
  --auth-font-display: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', serif;
  --auth-font-body: 'Noto Sans SC', 'Outfit', var(--font-family-sans, sans-serif);
  /* 与插画采样色一致 #BCE7FB，左右同色无断层 */
  --auth-bg: #bce7fb;
  --auth-surface: #e8f4f8;
  --auth-accent: #0891b2;
  --auth-accent-deep: #0e7490;
  --auth-accent-light: #22d3ee;
  --auth-accent-soft: rgba(8, 145, 178, 0.14);
  --auth-text-primary: #0c2a42;
  --auth-text-secondary: #3d6478;
  --auth-text-dim: #6a8fa3;
  --auth-text-inverse: #f0f9ff;
  --auth-border: rgba(8, 145, 178, 0.22);
  --auth-input-bg: #f4fafc;
  --auth-form-w: min(490px, 40vw);

  position: fixed;
  inset: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--auth-form-w);
  width: 100%;
  height: 100dvh;
  overflow: hidden;
  font-family: var(--auth-font-body);
  color: var(--auth-text-primary);
  background: var(--auth-bg);
}

.auth-visual {
  position: relative;
  z-index: 4;
  min-width: 0;
  height: 100%;
  overflow: visible;
  pointer-events: none;
  background: var(--auth-bg);
}

.auth-visual__img {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background-color: var(--auth-bg);
  background-position: left center;
  background-size: cover;
  background-repeat: no-repeat;
}

.stage-brand {
  position: absolute;
  top: clamp(24px, 3.5vh, 40px);
  left: clamp(24px, 3vw, 44px);
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  pointer-events: auto;
}

.stage-brand__mark {
  display: inline-flex;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 12px rgba(8, 145, 178, 0.22));
}

.stage-brand__en {
  margin: 0;
  font-family: var(--auth-font-brand);
  font-size: clamp(1.25rem, 1.8vw, 1.55rem);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1.1;
  color: var(--auth-text-primary);
}

.stage-brand__zh {
  margin: 4px 0 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--auth-accent-deep);
}

/* 贴在插画右侧空白条，不要用 left%（会锚进桌面） */
.stage-intro {
  position: absolute;
  top: clamp(132px, 22vh, 220px);
  right: -48px;
  left: auto;
  z-index: 2;
  width: min(340px, 38vw);
  pointer-events: auto;
  animation: stage-intro-in 480ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.stage-title {
  margin: 0;
  max-width: 10em;
  font-family: var(--auth-font-display);
  font-size: clamp(1.7rem, 2.7vw, 2.35rem);
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1.28;
  color: var(--auth-text-primary);
  text-wrap: balance;
}

.stage-lead {
  margin: 16px 0 0;
  max-width: 22em;
  font-size: clamp(14px, 1.15vw, 16px);
  font-weight: 500;
  line-height: 1.7;
  color: var(--auth-text-secondary);
  text-wrap: pretty;
}

.stage-features {
  list-style: none;
  margin: clamp(28px, 4vh, 44px) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: clamp(18px, 2.4vh, 26px);
}

.stage-feature {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.stage-feature__dot {
  width: 9px;
  height: 9px;
  margin-top: 8px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--auth-accent);
  box-shadow: 0 0 0 5px var(--auth-accent-soft);
}

.stage-feature__title {
  margin: 0;
  font-size: clamp(14px, 1.15vw, 16px);
  font-weight: 650;
  line-height: 1.35;
  color: var(--auth-text-primary);
}

.stage-feature__desc {
  margin: 6px 0 0;
  font-size: clamp(13px, 1.05vw, 14.5px);
  line-height: 1.55;
  color: var(--auth-text-dim);
}

/* 右侧纯色区：与插画背景同色，表单居中 */
.auth-form-zone {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  height: 100%;
  padding: clamp(24px, 4vh, 40px) clamp(20px, 2.5vw, 36px);
  background: var(--auth-bg);
}

.form-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 430px;
  margin: 0;
  padding: 28px 26px 22px;
  border-radius: 18px;
  background: var(--auth-surface);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    0 16px 36px rgba(12, 58, 78, 0.1),
    0 2px 8px rgba(8, 145, 178, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  animation: card-in 480ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.auth-panel {
  width: 100%;
}

.auth-slide-up-enter-active,
.auth-slide-up-leave-active {
  transition:
    opacity 200ms ease,
    transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}

.auth-slide-up-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.auth-slide-up-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.auth-head {
  margin-bottom: 20px;
}

.auth-title {
  margin: 0;
  font-family: var(--auth-font-brand);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
  color: var(--auth-text-primary);
}

.auth-sub {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--auth-text-dim);
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
  font-weight: 600;
  color: var(--auth-text-primary);
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
  width: 104px;
  min-height: 40px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--auth-border);
  background: var(--auth-input-bg);
  flex-shrink: 0;
  padding: 0;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.captcha-box:hover {
  border-color: var(--auth-accent);
  box-shadow: 0 0 0 3px var(--auth-accent-soft);
}

.captcha-box:focus-visible {
  outline: 2px solid var(--auth-accent);
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
  color: var(--auth-text-dim);
}

.auth-foot {
  margin-top: 16px;
}

.option-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 14px;
}

.check-label {
  color: var(--auth-text-secondary);
}

.link {
  font-size: 14px;
  color: var(--auth-accent);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  transition: color 150ms ease;
}

.link:hover {
  color: var(--auth-accent-deep);
}

.link:focus-visible {
  outline: 2px solid var(--auth-accent);
  outline-offset: 3px;
  border-radius: 2px;
}

.link--muted {
  color: var(--auth-text-dim);
  cursor: default;
}

.link--muted:hover {
  color: var(--auth-text-dim);
}

.form-card :deep(.ant-input-affix-wrapper),
.form-card :deep(.ant-input) {
  background: var(--auth-input-bg);
  border: 1px solid var(--auth-border);
  box-shadow: none;
  border-radius: 10px;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
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
  border-color: var(--auth-accent);
  box-shadow: 0 0 0 3px var(--auth-accent-soft);
}

.form-card :deep(.ant-input-affix-wrapper .ant-input:focus),
.form-card :deep(.ant-input-password .ant-input:focus) {
  border: none !important;
  box-shadow: none !important;
}

.form-card :deep(.ant-btn.submit-btn) {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-weight: 650;
  letter-spacing: 0.03em;
  color: var(--auth-text-inverse);
  background: linear-gradient(
    135deg,
    var(--auth-accent-light),
    var(--auth-accent) 48%,
    var(--auth-accent-deep)
  );
  box-shadow: 0 6px 16px rgba(8, 145, 178, 0.32);
  transition:
    transform 150ms ease,
    box-shadow 250ms ease,
    background 150ms ease;
}

.form-card :deep(.ant-btn.submit-btn:hover) {
  color: var(--auth-text-inverse);
  background: linear-gradient(135deg, var(--auth-accent), var(--auth-accent-deep)) !important;
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(8, 145, 178, 0.4);
}

.form-card :deep(.ant-btn.submit-btn:active) {
  transform: scale(0.98);
}

.form-card :deep(.ant-btn.submit-btn:focus-visible) {
  outline: 2px solid var(--auth-accent);
  outline-offset: 3px;
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-arrow {
  transition: transform 150ms ease;
}

.form-card :deep(.ant-btn.submit-btn:hover) .btn-arrow {
  transform: translateX(2px);
}

.switch-line {
  margin: 14px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--auth-text-dim);
}

.switch-line .link {
  font-size: 13px;
  font-weight: 550;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 入场只用透明度，避免覆盖定位用的 transform */
@keyframes stage-intro-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .form-card,
  .stage-intro {
    animation: none;
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

@media (max-width: 900px) {
  .auth-page {
    --auth-form-w: 100%;
    grid-template-columns: 1fr;
    grid-template-rows: minmax(200px, 34vh) auto;
    height: auto;
    min-height: 100dvh;
    overflow: auto;
  }

  .auth-visual {
    min-height: min(34vh, 280px);
  }

  .stage-brand {
    position: relative;
    top: auto;
    left: auto;
    padding: 20px 20px 0;
  }

  .stage-intro {
    position: relative;
    top: auto;
    left: auto;
    right: auto;
    width: auto;
    max-width: none;
    transform: none;
    padding: 16px 20px 0;
  }

  .stage-title {
    max-width: none;
  }

  .stage-features {
    margin-top: 18px;
  }

  .auth-form-zone {
    height: auto;
    padding: 20px 20px 32px;
  }

  .form-card {
    max-width: 430px;
    width: 100%;
  }
}

@media (max-width: 480px) {
  .form-card {
    padding: 24px 20px 20px;
    border-radius: 16px;
  }

  .auth-title {
    font-size: 22px;
  }

  .captcha-box {
    width: 96px;
  }

  .stage-brand__zh {
    letter-spacing: 0.12em;
  }
}
</style>
