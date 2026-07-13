<template>
  <div class="auth-page">
    <div class="auth-backdrop" aria-hidden="true"></div>

    <div class="form-card">
      <div class="card-segment card-segment--head">
        <div v-if="registerEnabled" class="auth-tabs" role="tablist">
              <button
                type="button"
                role="tab"
                class="auth-tab"
                :class="{ active: activeTab === 'login' }"
                :aria-selected="activeTab === 'login'"
                @click="switchTab('login')"
              >
                登录
              </button>
              <button
                type="button"
                role="tab"
                class="auth-tab"
                :class="{ active: activeTab === 'register' }"
                :aria-selected="activeTab === 'register'"
                @click="switchTab('register')"
              >
                注册
              </button>
            </div>
            <h2 v-else class="auth-title">登录</h2>
          </div>

          <!-- 段2：表单字段（固定三行高度） -->
          <div class="card-segment card-segment--fields">
            <el-form
              v-show="activeTab === 'login'"
              ref="loginFormRef"
              class="auth-form"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              size="large"
              @submit.prevent="handleLogin"
            >
              <el-form-item prop="account">
                <template #label><span class="label-text">账号 / 手机号 / 邮箱</span></template>
                <el-input
                  v-model.trim="loginForm.account"
                  placeholder="请输入账号、手机号或邮箱"
                  clearable
                >
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <template #label><span class="label-text">密码</span></template>
                <el-input
                  v-model.trim="loginForm.password"
                  placeholder="请输入密码"
                  show-password
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item class="form-item-last">
                <template #label><span class="label-text">验证码</span></template>
                <div class="captcha-row">
                  <el-input
                    v-model.trim="loginForm.captcha"
                    placeholder="请输入验证码"
                    clearable
                  >
                    <template #prefix><el-icon><CircleCheck /></el-icon></template>
                  </el-input>
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
              </el-form-item>
            </el-form>

            <el-form
              v-show="activeTab === 'register' && registerStep === 1"
              class="auth-form"
              :model="registerForm"
              label-position="top"
              size="large"
              @submit.prevent="onRegisterSubmit"
            >
              <el-form-item>
                <template #label><span class="label-text">注册方式</span></template>
                <div class="register-type-tabs" role="tablist">
                  <button
                    type="button"
                    role="tab"
                    class="register-type-tab"
                    :class="{ active: registerType === 'phone' }"
                    :aria-selected="registerType === 'phone'"
                    @click="switchRegisterType('phone')"
                  >
                    手机号
                  </button>
                  <button
                    type="button"
                    role="tab"
                    class="register-type-tab"
                    :class="{ active: registerType === 'email' }"
                    :aria-selected="registerType === 'email'"
                    @click="switchRegisterType('email')"
                  >
                    邮箱
                  </button>
                </div>
              </el-form-item>

              <el-form-item v-if="registerType === 'phone'">
                <template #label><span class="label-text">手机号</span></template>
                <el-input
                  v-model.trim="registerForm.phone"
                  placeholder="请输入手机号"
                  clearable
                >
                  <template #prefix><el-icon><Iphone /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item v-else>
                <template #label><span class="label-text">邮箱</span></template>
                <el-input
                  v-model.trim="registerForm.email"
                  placeholder="请输入邮箱"
                  clearable
                >
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item class="form-item-last">
                <template #label><span class="label-text">提示</span></template>
                <div class="auth-readonly-field">
                  <span class="auth-readonly-field__body">注册成功后系统将自动生成登录账号，</span><span class="auth-readonly-field__action">点击下一步</span><span class="auth-readonly-field__body">设置密码</span>
                </div>
              </el-form-item>
            </el-form>

            <el-form
              v-show="activeTab === 'register' && registerStep === 2"
              class="auth-form"
              :model="registerForm"
              label-position="top"
              size="large"
              @submit.prevent="onRegisterSubmit"
            >
              <el-form-item>
                <template #label><span class="label-text">密码</span></template>
                <el-input
                  v-model.trim="registerForm.password"
                  placeholder="至少 6 位"
                  show-password
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item>
                <template #label><span class="label-text">确认密码</span></template>
                <el-input
                  v-model.trim="registerForm.confirmPassword"
                  placeholder="请再次输入密码"
                  show-password
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item class="form-item-last">
                <template #label><span class="label-text">验证码</span></template>
                <div class="captcha-row">
                  <el-input
                    v-model.trim="registerForm.captcha"
                    placeholder="请输入验证码"
                    clearable
                  >
                    <template #prefix><el-icon><CircleCheck /></el-icon></template>
                  </el-input>
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
              </el-form-item>
            </el-form>
          </div>

          <!-- 段3：操作区 + 协议 -->
          <div class="card-segment card-segment--foot">
            <div v-show="activeTab === 'login'" class="auth-actions">
              <el-form-item class="form-item-compact">
                <div class="option-row">
                  <el-checkbox v-model="rememberMe">
                    <span class="check-label">记住我</span>
                  </el-checkbox>
                  <a class="link" href="javascript:void(0)">忘记密码？</a>
                </div>
              </el-form-item>
              <el-button class="submit-btn" :loading="loginLoading" @click="handleLogin">
                <span v-if="!loginLoading" class="btn-content">
                  <span>登录</span>
                  <el-icon class="btn-arrow"><ArrowRight /></el-icon>
                </span>
                <span v-else>正在验证身份...</span>
              </el-button>
            </div>

            <div v-show="activeTab === 'register' && registerStep === 1" class="auth-actions">
              <el-form-item class="form-item-compact">
                <div class="option-row">
                  <span aria-hidden="true"></span>
                  <a class="link" href="javascript:void(0)" @click="switchTab('login')">
                    已有账号？登录
                  </a>
                </div>
              </el-form-item>
              <el-button class="submit-btn" @click="nextRegisterStep">
                <span class="btn-content">
                  <span>下一步</span>
                  <el-icon class="btn-arrow"><ArrowRight /></el-icon>
                </span>
              </el-button>
            </div>

            <div v-show="activeTab === 'register' && registerStep === 2" class="auth-actions">
              <el-form-item class="form-item-compact">
                <div class="option-row">
                  <button type="button" class="text-btn" @click="prevRegisterStep">
                    上一步
                  </button>
                  <span aria-hidden="true"></span>
                </div>
              </el-form-item>
              <el-button class="submit-btn" :loading="registerLoading" @click="handleRegister">
                <span v-if="!registerLoading" class="btn-content">
                  <span>立即注册并登录</span>
                  <el-icon class="btn-arrow"><ArrowRight /></el-icon>
                </span>
                <span v-else>正在注册并登录...</span>
              </el-button>
            </div>

            <p class="footer-hint">
              {{ activeTab === 'login' || !registerEnabled ? '登录' : '注册' }}系统账号
            </p>
          </div>
    </div>
  </div>
</template>

<script setup>
/** 登录/注册页：对接后端 /auth 与验证码接口 */
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone, Message, CircleCheck, ArrowRight } from '@element-plus/icons-vue'
import { loginApi, registerApi } from '@/apis/system/AuthApi'
import { fetchCaptchaBase64Api } from '@/apis/common/CaptchaApi'
import { useUserStore } from '@/stores/userStore'
import { buildRoutes, resolveDefaultPath } from '@/router/dynamicRoutes'
import appRouter from '@/router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { setAuth, setUserLoginInfo } = userStore

// --- Tab 与步骤 ---
const registerEnabled = ref(true)
const activeTab = ref(route.query.tab === 'register' ? 'register' : 'login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const rememberMe = ref(false)
const loginFormRef = ref(null)
// --- 验证码 ---
const captchaBase64 = ref('')
const captchaToken = ref('')
const captchaTimestamp = ref(0)
const registerStep = ref(1)  // 注册分两步：基本信息 -> 密码
const registerType = ref('phone')  // phone | email，与后端 Register 二选一

const loginForm = reactive({
  account: '',
  password: '',
  captcha: ''
})

const registerForm = reactive({
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const loginRules = {
  account: [{ required: true, message: '请输入账号、手机号或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'login') {
      activeTab.value = 'login'
    } else if (tab === 'register' && registerEnabled.value) {
      activeTab.value = 'register'
    } else if (tab === 'register' && !registerEnabled.value) {
      switchTab('login')
    }
  }
)

function switchTab(tab) {
  if (tab === 'register' && !registerEnabled.value) return
  if (activeTab.value === tab) return
  activeTab.value = tab
  registerStep.value = 1
  registerType.value = 'phone'
  registerForm.phone = ''
  registerForm.email = ''
  router.replace({ path: '/auth', query: { ...route.query, tab } })
  refreshCaptcha()
}

function switchRegisterType(type) {
  if (registerType.value === type) return
  registerType.value = type
  registerForm.phone = ''
  registerForm.email = ''
}

async function refreshCaptcha() {
  try {
    const result = await fetchCaptchaBase64Api()
    captchaBase64.value = result.data.img
    captchaToken.value = result.data.token
    captchaTimestamp.value = result.data.timestamp
  } catch (error) {
    ElMessage.error(error.message || '获取验证码失败')
    captchaBase64.value = ''
  }
}

function nextRegisterStep() {
  if (registerType.value === 'phone') {
    if (!registerForm.phone?.trim()) {
      ElMessage.warning('请输入手机号')
      return
    }
    if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }
  } else {
    if (!registerForm.email?.trim()) {
      ElMessage.warning('请输入邮箱')
      return
    }
    if (!EMAIL_PATTERN.test(registerForm.email)) {
      ElMessage.warning('请输入正确的邮箱')
      return
    }
  }
  registerStep.value = 2
  registerForm.captcha = ''
  refreshCaptcha()
}

function prevRegisterStep() {
  registerStep.value = 1
  registerForm.captcha = ''
  refreshCaptcha()
}

function onRegisterSubmit() {
  if (registerStep.value === 1) {
    nextRegisterStep()
  } else {
    handleRegister()
  }
}

/** 登录成功后：保存 token 与 sys_user 核心字段并跳转仪表盘 */
function completeLogin(data) {
  if (!data?.token) {
    throw new Error('登录成功但未获取到令牌')
  }
  if (!data?.userId) {
    throw new Error('登录成功但未获取到用户ID')
  }

  setAuth(data.token, {
    userId: data.userId,
    account: data.account,
    username: data.username,
  })
  setUserLoginInfo(data)
  buildRoutes(appRouter)
  ElMessage.success('登录成功')
  const redirect =
    typeof route.query.redirect === 'string' ? route.query.redirect : resolveDefaultPath('/dashboard')
  router.replace(redirect)
}

async function handleLogin() {
  if (!loginForm.captcha?.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  loginLoading.value = true
  try {
    const result = await loginApi({
      account: loginForm.account,
      password: loginForm.password,
      captchaCode: loginForm.captcha,
      captchaToken: captchaToken.value,
      captchaTimestamp: captchaTimestamp.value,
    })
    completeLogin(result?.data || {})
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
    refreshCaptcha()
    loginForm.captcha = ''
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.password?.trim()) {
    ElMessage.warning('请输入密码')
    return
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 20) {
    ElMessage.warning('密码长度需在 6-20 位之间')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入密码不一致')
    return
  }
  if (!registerForm.captcha?.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }

  registerLoading.value = true
  try {
    const payload = {
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      captchaCode: registerForm.captcha,
      captchaToken: captchaToken.value,
      captchaTimestamp: captchaTimestamp.value,
    }
    if (registerType.value === 'phone') {
      payload.phone = registerForm.phone
    } else {
      payload.email = registerForm.email
    }

    const result = await registerApi(payload)
    const data = result?.data || {}
    if (!data.account) {
      throw new Error('注册成功但未获取到系统账号')
    }
    ElMessage.success(`注册成功，系统账号：${data.account}`)
    loginForm.account = data.account
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerStep.value = 1
    switchTab('login')
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
    refreshCaptcha()
    registerForm.captcha = ''
  } finally {
    registerLoading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.auth-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
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
    135deg,
    rgba(248, 245, 240, 0.82) 0%,
    rgba(248, 245, 240, 0.55) 45%,
    rgba(248, 245, 240, 0.35) 100%
  );
}

.form-card {
  position: relative;
  z-index: 1;
  --auth-accent: #0891b2;
  --auth-accent-deep: #0e7490;
  --auth-accent-light: #22d3ee;
  --auth-accent-soft: rgba(34, 184, 207, 0.16);
  --auth-surface: rgba(248, 252, 255, 0.97);
  --auth-tabs-bg: rgba(12, 48, 78, 0.07);
  --auth-input-bg: rgba(255, 255, 255, 0.9);
  --auth-border: rgba(34, 184, 207, 0.24);
  --auth-border-strong: rgba(34, 184, 207, 0.4);
  --auth-text-primary: #0c2a42;
  --auth-text-secondary: #3d6478;
  --auth-text-dim: #6a8fa3;
  --auth-text-inverse: #f0f9ff;

  width: 100%;
  max-width: 440px;
  padding: 32px 32px 24px;
  background: var(--auth-surface);
  border: 1px solid var(--auth-border-strong);
  border-radius: 16px;
  box-shadow:
    0 24px 48px rgba(6, 36, 64, 0.18),
    0 8px 24px rgba(34, 184, 207, 0.12);
  backdrop-filter: blur(12px);
}

.card-segment--head {
  flex-shrink: 0;
}

.auth-title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  color: var(--auth-text-primary);
}

.auth-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  padding: 4px;
  margin-bottom: 16px;
  background: var(--auth-tabs-bg);
  border-radius: 8px;
  border: 1px solid var(--auth-border);
}

.register-type-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  width: 100%;
  padding: 4px;
  background: var(--auth-tabs-bg);
  border-radius: 8px;
  border: 1px solid var(--auth-border);
}

.register-type-tab {
  min-height: 34px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--auth-text-secondary);
  background: transparent;
  cursor: pointer;
}

.register-type-tab:hover {
  color: var(--auth-accent);
}

.register-type-tab.active {
  color: var(--auth-text-inverse);
  font-weight: 600;
  background: linear-gradient(135deg, var(--auth-accent-light), var(--auth-accent));
  box-shadow: 0 4px 14px rgba(8, 145, 178, 0.28);
}

.auth-tab {
  min-height: 36px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--auth-text-secondary);
  background: transparent;
  cursor: pointer;
}

.auth-tab:hover {
  color: var(--auth-accent);
}

.auth-tab:focus-visible {
  outline: 2px solid var(--auth-accent);
  outline-offset: 2px;
}

.auth-tab.active {
  color: var(--auth-text-inverse);
  font-weight: 600;
  background: linear-gradient(135deg, var(--auth-accent-light), var(--auth-accent));
  box-shadow: 0 4px 14px rgba(8, 145, 178, 0.28);
}

.auth-form {
  margin: 0;
}

.auth-readonly-field {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--auth-text-secondary);
  background: #f5f7fa;
  border: 1px solid var(--auth-border);
}

.form-card :deep(.form-item-compact .el-form-item__label) {
  display: none;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--auth-text-primary);
}

.form-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.form-card :deep(.el-form-item.form-item-last) {
  margin-bottom: 0;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-box {
  width: 110px;
  min-height: 40px;
  border-radius: 6px;
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
  font-size: 12px;
  color: var(--auth-text-dim);
}

.option-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.text-btn,
.link {
  font-size: 14px;
  color: var(--auth-accent);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
}

.form-card :deep(.el-input__wrapper) {
  background: var(--auth-input-bg);
  border: 1px solid var(--auth-border-strong);
  box-shadow: none;
}

.form-card :deep(.el-input__wrapper.is-focus) {
  border-color: var(--auth-accent);
  box-shadow: 0 0 0 3px var(--auth-accent-soft);
}

.form-card :deep(.el-button.submit-btn) {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border: none;
  color: var(--auth-text-inverse);
  background: linear-gradient(135deg, var(--auth-accent-light), var(--auth-accent));
  box-shadow: 0 6px 18px rgba(8, 145, 178, 0.3);
}

.form-card :deep(.el-button.submit-btn:hover) {
  background: linear-gradient(135deg, var(--auth-accent), var(--auth-accent-deep));
}

.btn-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.footer-hint {
  text-align: center;
  font-size: 12px;
  color: var(--auth-text-dim);
  margin: 12px 0 0;
}

@media (max-width: 480px) {
  .form-card {
    padding: 24px 20px 20px;
  }

  .captcha-box {
    width: 96px;
  }
}
</style>
