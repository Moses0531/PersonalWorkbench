<template>
  <div class="auth-page">
    <div class="auth-backdrop" aria-hidden="true"></div>

    <div class="form-card">
      <Transition name="auth-slide-up" mode="out-in">
        <div :key="activeTab" class="auth-panel">
          <header class="auth-head">
            <h1 class="auth-title">{{ isLogin ? '登录' : '注册' }}</h1>
            <p class="auth-sub">
              {{ isLogin ? '使用系统账号、手机号或邮箱登录' : '填写资料，系统将自动生成登录账号' }}
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
                <a class="link" href="javascript:void(0)">忘记密码？</a>
              </div>
              <a-button
                class="submit-btn"
                type="primary"
                :loading="loginLoading"
                @click="handleLogin"
              >
                <span v-if="!loginLoading" class="btn-content">
                  <span>登录</span>
                  <ArrowRightOutlined class="btn-arrow" />
                </span>
                <span v-else>正在验证身份...</span>
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
  </div>
</template>

<script setup>
/** 登录/注册页：标题模式切换，表单自然高度，不再用 Tab 同框硬撑 */
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
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 100dvh;
  padding: 24px clamp(24px, 6vw, 96px) 24px 24px;
  overflow: auto;
}

.auth-backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-image: url('/BackgroundAuthPage.png');
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
}

.auth-backdrop::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    rgba(12, 42, 66, 0.28) 0%,
    rgba(14, 116, 144, 0.18) 42%,
    rgba(240, 247, 251, 0.55) 72%,
    rgba(248, 252, 255, 0.72) 100%
  );
}

.form-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 460px;
  margin: 0;
  padding: 36px 32px 28px;
  overflow: hidden;
  --auth-accent: var(--color-accent, #0891b2);
  --auth-accent-deep: var(--color-accent-deep, #0e7490);
  --auth-accent-light: var(--color-accent-light, #22d3ee);
  --auth-accent-soft: rgba(34, 184, 207, 0.16);
  --auth-surface: rgba(248, 252, 255, 0.97);
  --auth-input-bg: rgba(255, 255, 255, 0.92);
  --auth-border: rgba(34, 184, 207, 0.24);
  --auth-border-strong: rgba(34, 184, 207, 0.4);
  --auth-text-primary: var(--color-text-primary, #0c2a42);
  --auth-text-secondary: var(--color-text-body, #3d6478);
  --auth-text-dim: var(--color-text-secondary, #6a8fa3);
  --auth-text-inverse: #f0f9ff;

  background: var(--auth-surface);
  border: 1px solid var(--auth-border-strong);
  border-radius: var(--radius-xl, 16px);
  box-shadow:
    0 24px 48px rgba(6, 36, 64, 0.18),
    0 8px 24px rgba(34, 184, 207, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px) saturate(140%);
}

.auth-panel {
  width: 100%;
}


.auth-slide-up-enter-active,
.auth-slide-up-leave-active {
  transition:
    opacity 50ms ease,
    transform 50ms ease;
}

.auth-slide-up-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.auth-slide-up-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (prefers-reduced-motion: reduce) {
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
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.03em;
  line-height: 1.2;
  color: var(--auth-text-primary);
}

.auth-sub {
  margin: 8px 0 0;
  font-size: 14px;
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
  font-weight: 550;
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
  width: 108px;
  min-height: 40px;
  border-radius: var(--radius-control, 10px);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--auth-border-strong);
  background: var(--auth-input-bg);
  flex-shrink: 0;
  padding: 0;
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
  color: var(--auth-text-secondary);
}

.link {
  font-size: 14px;
  color: var(--auth-accent);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
}

.link:hover {
  color: var(--auth-accent-deep);
}

.form-card :deep(.ant-input-affix-wrapper),
.form-card :deep(.ant-input) {
  background: var(--auth-input-bg);
  border: 1px solid var(--auth-border-strong);
  box-shadow: none;
  border-radius: var(--radius-control, 10px);
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
  border-radius: var(--radius-control, 10px);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--auth-text-inverse);
  background: linear-gradient(135deg, var(--auth-accent-light), var(--auth-accent));
  box-shadow: 0 6px 18px rgba(8, 145, 178, 0.3);
  transition:
    transform var(--transition-fast, 150ms ease),
    box-shadow var(--transition-spring, 300ms cubic-bezier(0.16, 1, 0.3, 1));
}

.form-card :deep(.ant-btn.submit-btn:hover) {
  color: var(--auth-text-inverse);
  background: linear-gradient(135deg, var(--auth-accent), var(--auth-accent-deep)) !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 22px rgba(8, 145, 178, 0.36);
}

.form-card :deep(.ant-btn.submit-btn:active) {
  transform: scale(0.98);
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.switch-line {
  margin: 16px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--auth-text-dim);
}

.switch-line .link {
  font-size: 13px;
  font-weight: 550;
}

@media (max-width: 900px) {
  .auth-page {
    justify-content: center;
    padding: 24px;
  }

  .auth-backdrop::after {
    background: linear-gradient(
      180deg,
      rgba(12, 42, 66, 0.2) 0%,
      rgba(240, 247, 251, 0.72) 55%,
      rgba(248, 252, 255, 0.88) 100%
    );
  }
}

@media (max-width: 480px) {
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
}
</style>
