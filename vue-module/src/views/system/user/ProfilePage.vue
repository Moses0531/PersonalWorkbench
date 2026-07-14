<script setup>
/** 个人中心：资料修改、头像上传与密码变更 */
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  CopyOutlined,
  LockOutlined,
  MailOutlined,
  MobileOutlined,
  PhoneOutlined,
} from '@ant-design/icons-vue'
import {
  getCurrentUserProfileApi,
  updateUserProfileApi,
  changePasswordApi,
  uploadAvatarApi,
} from '@/apis/system/user/UserProfileApi'
import { hasPermission } from '@/utils/menu'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const canModify = computed(() => hasPermission('profile:modify'))

// --- 单字段编辑弹窗 ---
const savingField = ref(false)
const fieldDialogVisible = ref(false)
const editingField = ref(null)
const fieldDialogKey = ref(null)
const fieldDraft = ref('')
const passwordDialogVisible = ref(false)

const fieldDialogTitles = {
  email: '修改安全邮箱',
  phone: '修改安全手机号',
  phoneLogin: '修改手机号登录',
  displayName: '修改昵称',
  realName: '修改真实姓名',
  sex: '修改性别',
  birthday: '修改生日',
}
const avatarError = ref(false)

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const ROLE_LABELS = {
  0: '超级管理员',
  1: '管理员',
  2: '普通用户',
}

// --- 用户资料（与 SysUser 字段对齐） ---
const userInfo = reactive({
  userId: null,
  account: '',
  username: '',
  realName: '',
  phone: '',
  email: '',
  sex: '0',
  avatar: '',
  birthday: '',
  roleId: null,
  createTime: '',
  lastLoginTime: '',
})

const sexOptions = [
  { label: '男', value: '0' },
  { label: '女', value: '1' },
]

/** 相对路径头像拼 VITE_APP_BASE_API 前缀 */
function resolveAvatarUrl(raw) {
  if (!raw) return ''
  if (/^https?:\/\//.test(raw)) return raw
  const base = (import.meta.env.VITE_APP_BASE_API || '/api').replace(/\/$/, '')
  return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
}

const avatarSrc = computed(() => resolveAvatarUrl(userInfo.avatar))
watch(() => userInfo.avatar, () => { avatarError.value = false })

function formatBirthDate(s) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTime(s) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 展示用脱敏，编辑弹窗仍用明文 userInfo */
function maskEmail(email) {
  if (!email) return ''
  const [name, domain] = email.split('@')
  if (!domain) return email
  const visible = name.slice(0, Math.min(2, name.length))
  return `${visible}${'*'.repeat(Math.max(name.length - visible.length, 2))}@${domain}`
}

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

const sexLabel = computed(() => sexOptions.find((i) => i.value === userInfo.sex)?.label || '-')

const ageText = computed(() => {
  if (!userInfo.birthday) return '-'
  const birth = new Date(userInfo.birthday)
  if (isNaN(birth.getTime())) return '-'
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const m = today.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age -= 1
  return age >= 0 ? `${age} 岁` : '-'
})

const displayName = computed(() => userInfo.username || userInfo.account || '-')
const roleDisplayName = computed(() => {
  if (userInfo.roleId == null) return '-'
  return ROLE_LABELS[userInfo.roleId] ?? `角色 ${userInfo.roleId}`
})
const fieldDialogTitle = computed(() => fieldDialogTitles[fieldDialogKey.value] || '修改资料')

function buildProfilePayload() {
  return {
    userId: userInfo.userId,
    username: userInfo.username,
    realName: userInfo.realName,
    phone: userInfo.phone,
    email: userInfo.email,
    sex: userInfo.sex,
    birthday: userInfo.birthday ? `${userInfo.birthday}T00:00:00` : null,
  }
}

function syncStoredUser() {
  if (!userStore.user) return
  userStore.setAuth(userStore.token, {
    ...userStore.user,
    username: userInfo.username,
    account: userInfo.account,
    avatar: userInfo.avatar,
  })
}

async function loadUserInfo() {
  loading.value = true
  try {
    const r = await getCurrentUserProfileApi()
    const d = r?.data || {}
    Object.assign(userInfo, {
      userId: d.userId ?? null,
      account: d.account || '',
      username: d.username || '',
      realName: d.realName || '',
      phone: d.phone || '',
      email: d.email || '',
      sex: d.sex != null && d.sex !== '' ? String(d.sex) : '0',
      avatar: d.avatar || '',
      birthday: formatBirthDate(d.birthday),
      roleId: d.roleId ?? null,
      createTime: formatDateTime(d.createTime),
      lastLoginTime: formatDateTime(d.lastLoginTime),
    })
    syncStoredUser()
    closeFieldDialog()
  } catch (e) { message.error(e.message || '获取用户信息失败') }
  finally { loading.value = false }
}

function openFieldDialog(saveField, dialogKey = saveField) {
  editingField.value = saveField
  fieldDialogKey.value = dialogKey
  fieldDraft.value = userInfo[saveField] ?? ''
  fieldDialogVisible.value = true
}

function closeFieldDialog() {
  fieldDialogVisible.value = false
  editingField.value = null
  fieldDialogKey.value = null
  fieldDraft.value = ''
}

function validateField(field, value) {
  if (field === 'username' && !String(value).trim()) {
    message.warning('请输入昵称')
    return false
  }
  if (field === 'realName' && !String(value).trim()) {
    message.warning('请输入真实姓名')
    return false
  }
  if (field === 'phone') {
    if (!String(value).trim()) { message.warning('请输入手机号'); return false }
    if (!/^1\d{10}$/.test(String(value).trim())) { message.warning('请输入正确的手机号'); return false }
  }
  if (field === 'email' && String(value).trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value).trim())) {
    message.warning('请输入正确的邮箱格式')
    return false
  }
  return true
}

async function confirmFieldEdit() {
  const field = editingField.value
  if (!field) return
  if (!validateField(field, fieldDraft.value)) return
  savingField.value = true
  try {
    userInfo[field] = fieldDraft.value
    await updateUserProfileApi(buildProfilePayload())
    syncStoredUser()
    message.success('保存成功')
    closeFieldDialog()
  } catch (e) {
    message.error(e.message || '保存失败')
    await loadUserInfo()
  } finally {
    savingField.value = false
  }
}

async function handleAvatarUpload(opts) {
  const f = opts?.file
  if (!f) { message.error('未选择有效文件'); return }
  try {
    const r = await uploadAvatarApi(f)
    userInfo.avatar = r?.data ?? r
    syncStoredUser()
    message.success('头像更新成功')
    await loadUserInfo()
    opts?.onSuccess?.(r)
  } catch (e) {
    message.error(e.message || '头像上传失败')
    opts?.onError?.(e)
  }
}

function beforeUpload(f) {
  if (!f.type.startsWith('image/')) { message.error('只能上传图片文件'); return false }
  return true
}

function openPasswordDialog() {
  passwordDialogVisible.value = true
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
}

/** 改密成功后强制回登录页，避免旧 token 继续可用 */
async function changePassword() {
  if (!passwordForm.oldPassword) { message.warning('请输入原密码'); return }
  if (!passwordForm.newPassword) { message.warning('请输入新密码'); return }
  if (passwordForm.newPassword.length < 6) { message.warning('新密码长度不能少于6位'); return }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) { message.warning('两次输入的密码不一致'); return }
  try {
    await changePasswordApi({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    message.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    setTimeout(() => { window.location.href = '/auth' }, 1500)
  } catch (e) { message.error(e.message || '密码修改失败') }
}

async function copyAccountId() {
  if (userInfo.userId == null) return
  try {
    await navigator.clipboard.writeText(String(userInfo.userId))
    message.success('已复制账号 ID')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

onMounted(loadUserInfo)
</script>

<template>
  <a-spin :spinning="loading"><div class="profile-page">
    <header class="profile-page__header">
      <h1 class="profile-page__title">个人中心</h1>
      <p class="profile-page__desc">查看与更新账号资料、联系方式及安全选项</p>
    </header>

    <!-- 基本信息 -->
    <section class="profile-section" aria-labelledby="basic-info-title">
      <h2 id="basic-info-title" class="profile-section__title">基本信息</h2>
      <div class="basic-card">
        <div class="basic-card__avatar">
          <a-upload
            v-if="canModify"
            class="avatar-upload"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="handleAvatarUpload"
          >
            <div class="avatar-upload__ring">
              <img
                v-if="avatarSrc && !avatarError"
                :src="avatarSrc"
                class="avatar-upload__img"
                alt="用户头像"
                @error="avatarError = true"
              />
              <div v-else class="avatar-upload__placeholder" aria-hidden="true">
                <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="32" cy="24" r="10" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M14 52c3-10 10-15 18-15s15 5 18 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </div>
              <span class="avatar-upload__hint">更换头像</span>
            </div>
          </a-upload>
          <div v-else class="avatar-upload avatar-upload--readonly">
            <div class="avatar-upload__ring">
              <img
                v-if="avatarSrc && !avatarError"
                :src="avatarSrc"
                class="avatar-upload__img"
                alt="用户头像"
                @error="avatarError = true"
              />
              <div v-else class="avatar-upload__placeholder" aria-hidden="true">
                <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="32" cy="24" r="10" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M14 52c3-10 10-15 18-15s15 5 18 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div class="basic-card__fields">
          <dl class="field-grid">
            <div class="field-item">
              <dt class="field-item__label">昵称</dt>
              <dd class="field-item__value">
                <span>{{ displayName }}</span>
                <button
                  v-if="canModify"
                  type="button"
                  class="icon-action"
                  title="修改昵称"
                  @click="openFieldDialog('username', 'displayName')"
                >
                  <EditOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">账号 ID</dt>
              <dd class="field-item__value field-item__value--mono">
                <span>{{ userInfo.userId ?? '-' }}</span>
                <button
                  v-if="userInfo.userId != null"
                  type="button"
                  class="icon-action"
                  title="复制账号 ID"
                  @click="copyAccountId"
                >
                  <CopyOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">安全手机号</dt>
              <dd class="field-item__value">
                <span>{{ maskPhone(userInfo.phone) || '未设置' }}</span>
                <button
                  v-if="canModify"
                  type="button"
                  class="icon-action"
                  title="修改安全手机号"
                  @click="openFieldDialog('phone')"
                >
                  <EditOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">登录名</dt>
              <dd class="field-item__value">
                <span>{{ userInfo.account || '-' }}</span>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">安全邮箱</dt>
              <dd class="field-item__value">
                <template v-if="userInfo.email">
                  <span>{{ maskEmail(userInfo.email) }}</span>
                  <button
                    v-if="canModify"
                    type="button"
                    class="icon-action"
                    title="修改安全邮箱"
                    @click="openFieldDialog('email')"
                  >
                    <EditOutlined />
                  </button>
                </template>
                <template v-else>
                  <span class="field-item__empty">未设置</span>
                  <button
                    v-if="canModify"
                    type="button"
                    class="text-link"
                    @click="openFieldDialog('email')"
                  >
                    绑定邮箱
                  </button>
                </template>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">性别</dt>
              <dd class="field-item__value">
                <span>{{ sexLabel }}</span>
                <button
                  v-if="canModify"
                  type="button"
                  class="icon-action"
                  title="修改性别"
                  @click="openFieldDialog('sex')"
                >
                  <EditOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">当前角色</dt>
              <dd class="field-item__value">
                <span class="role-badge">{{ roleDisplayName }}</span>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">真实姓名</dt>
              <dd class="field-item__value">
                <span>{{ userInfo.realName || '未填写' }}</span>
                <button
                  v-if="canModify"
                  type="button"
                  class="icon-action"
                  title="修改真实姓名"
                  @click="openFieldDialog('realName')"
                >
                  <EditOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">生日 / 年龄</dt>
              <dd class="field-item__value">
                <span>{{ userInfo.birthday ? `${userInfo.birthday} · ${ageText}` : ageText }}</span>
                <button
                  v-if="canModify"
                  type="button"
                  class="icon-action"
                  title="修改生日"
                  @click="openFieldDialog('birthday')"
                >
                  <EditOutlined />
                </button>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">注册时间</dt>
              <dd class="field-item__value field-item__value--mono">
                <span>{{ userInfo.createTime || '-' }}</span>
              </dd>
            </div>

            <div class="field-item">
              <dt class="field-item__label">上次登录</dt>
              <dd class="field-item__value field-item__value--mono">
                <span>{{ userInfo.lastLoginTime || '-' }}</span>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </section>

    <!-- 安全设置 -->
    <section class="profile-section" aria-labelledby="security-title">
      <h2 id="security-title" class="profile-section__title">安全设置</h2>
      <ul class="security-list">
        <li class="security-item">
          <div class="security-item__icon security-item__icon--primary" aria-hidden="true">
            <LockOutlined />
          </div>
          <div class="security-item__body">
            <h3 class="security-item__title">登录密码</h3>
            <p class="security-item__desc">高强度密码有助于保护账号安全，建议定期更换登录密码。</p>
          </div>
          <button
            v-if="canModify"
            type="button"
            class="action-btn action-btn--primary"
            @click="openPasswordDialog"
          >
            修改
          </button>
        </li>

        <li class="security-item">
          <div class="security-item__icon security-item__icon--email" aria-hidden="true">
            <MailOutlined />
          </div>
          <div class="security-item__body">
            <h3 class="security-item__title">
              安全邮箱
              <span v-if="userInfo.email" class="status-tag status-tag--ok">已绑定</span>
              <span v-else class="status-tag status-tag--warn">未绑定</span>
            </h3>
            <p class="security-item__desc">
              {{ userInfo.email ? `当前邮箱 ${maskEmail(userInfo.email)}，可用于找回密码与接收通知。` : '绑定邮箱后可用于找回密码与接收系统通知。' }}
            </p>
          </div>
          <button
            v-if="canModify"
            type="button"
            class="action-btn action-btn--primary"
            @click="openFieldDialog('email')"
          >
            {{ userInfo.email ? '修改' : '绑定' }}
          </button>
        </li>

        <li class="security-item">
          <div class="security-item__icon security-item__icon--phone" aria-hidden="true">
            <MobileOutlined />
          </div>
          <div class="security-item__body">
            <h3 class="security-item__title">
              安全手机号
              <span v-if="userInfo.phone" class="status-tag status-tag--ok">已绑定</span>
              <span v-else class="status-tag status-tag--warn">未绑定</span>
            </h3>
            <p class="security-item__desc">
              {{ userInfo.phone ? `当前号码 ${maskPhone(userInfo.phone)}，用于账号验证与安全提醒。` : '绑定手机号后可接收验证码与安全提醒。' }}
            </p>
          </div>
          <button
            v-if="canModify"
            type="button"
            class="action-btn action-btn--primary"
            @click="openFieldDialog('phone')"
          >
            {{ userInfo.phone ? '修改' : '绑定' }}
          </button>
        </li>

        <li class="security-item">
          <div class="security-item__icon security-item__icon--login" aria-hidden="true">
            <PhoneOutlined />
          </div>
          <div class="security-item__body">
            <h3 class="security-item__title">手机号登录</h3>
            <p class="security-item__desc">
              {{ userInfo.phone ? `已启用手机号 ${maskPhone(userInfo.phone)} 作为登录凭证。` : '设置可用于登录的手机号码。' }}
            </p>
          </div>
          <button
            v-if="canModify"
            type="button"
            class="action-btn action-btn--primary"
            @click="openFieldDialog('phone', 'phoneLogin')"
          >
            {{ userInfo.phone ? '更改' : '设置' }}
          </button>
        </li>
      </ul>
    </section>

    <!-- 字段编辑弹窗 -->
    <a-modal v-model:open="fieldDialogVisible" :title="null" width="420px" class="field-dialog" :mask-closable="false" :footer="null" @after-close="closeFieldDialog">
      <div class="field-dialog__header">
        <h3>{{ fieldDialogTitle }}</h3>
      </div>
      <div class="field-dialog__body">
        <a-input
          v-if="editingField === 'email'"
          v-model:value="fieldDraft"
          placeholder="请输入邮箱"
        />
        <a-input
          v-else-if="editingField === 'phone'"
          v-model:value="fieldDraft"
          placeholder="请输入手机号"
          maxlength="11"
        />
        <a-input
          v-else-if="editingField === 'username'"
          v-model:value="fieldDraft"
          placeholder="请输入昵称"
        />
        <a-input
          v-else-if="editingField === 'realName'"
          v-model:value="fieldDraft"
          placeholder="请输入真实姓名"
        />
        <a-select v-else-if="editingField === 'sex'" v-model:value="fieldDraft" style="width: 100%" :options="sexOptions" />
        <a-date-picker
          v-else-if="editingField === 'birthday'"
          v-model:value="fieldDraft"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </div>
      
        <div class="field-dialog__footer">
          <button type="button" class="btn btn--outline" :disabled="savingField" @click="closeFieldDialog">取消</button>
          <button type="button" class="btn btn--primary" :disabled="savingField" @click="confirmFieldEdit">保存</button>
        </div>
      </a-modal>

    <!-- 密码弹窗 -->
    <a-modal v-model:open="passwordDialogVisible" :title="null" width="420px" class="password-dialog" :mask-closable="false" :footer="null">
      <div class="password-dialog__header">
        <div class="password-dialog__icon">
          <LockOutlined />
        </div>
        <h3>修改密码</h3>
        <p>为了您的账户安全，修改密码后需要重新登录</p>
      </div>
      <a-form :model="passwordForm" layout="vertical" class="password-form">
        <a-form-item label="原密码"><a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" /></a-form-item>
        <a-form-item label="新密码"><a-input-password v-model:value="passwordForm.newPassword" placeholder="至少6位" /></a-form-item>
        <a-form-item label="确认新密码"><a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" /></a-form-item>
      </a-form>
      
        <div class="password-dialog__footer">
          <button type="button" class="btn btn--outline" @click="passwordDialogVisible = false">取消</button>
          <button type="button" class="btn btn--primary" @click="changePassword">确认修改</button>
        </div>
      </a-modal>
  </div>
</a-spin>
</template>

<style scoped>
.profile-page {
  --profile-max: 1080px;
  --profile-radius: var(--radius-panel, 18px);
  --profile-primary: var(--color-accent);
  --profile-primary-deep: var(--color-accent-deep);

  min-height: 100%;
  padding: 28px 32px 52px;
  background:
    var(--color-zone-content-glow),
    var(--color-bg);
  font-family: var(--font-family-sans);
  -webkit-font-smoothing: antialiased;
}

.profile-page__header {
  max-width: var(--profile-max);
  margin: 0 auto 28px;
}

.profile-page__title {
  margin: 0;
  font-size: 24px;
  font-weight: var(--font-bold);
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
  text-wrap: balance;
}

.profile-page__desc {
  margin: 8px 0 0;
  font-size: var(--text-base, 13px);
  color: var(--color-text-secondary);
  line-height: 1.6;
  max-width: 42rem;
}

.profile-section {
  max-width: var(--profile-max);
  margin: 0 auto 24px;
}

.profile-section__title {
  margin: 0 0 14px;
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}

/* 基本信息卡片 */
.basic-card {
  display: flex;
  align-items: flex-start;
  gap: 36px;
  padding: 28px 32px 32px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--profile-radius);
  box-shadow: var(--shadow-lg);
}

.basic-card__avatar {
  flex-shrink: 0;
}

.avatar-upload :deep(.ant-upload) {
  display: block;
}

.avatar-upload__ring {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--color-accent-muted);
  border: 2px solid var(--color-border);
  transition: border-color var(--transition-normal), transform var(--transition-normal);
}

.avatar-upload:not(.avatar-upload--readonly):hover .avatar-upload__ring {
  border-color: var(--color-accent-glow);
  transform: scale(1.02);
}

.avatar-upload__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-dim);
}

.avatar-upload__placeholder svg {
  width: 52px;
  height: 52px;
}

.avatar-upload__hint {
  position: absolute;
  inset: auto 0 0;
  padding: 6px 0 8px;
  font-size: 10px;
  font-weight: var(--font-medium);
  text-align: center;
  color: #fff;
  background: linear-gradient(to top, rgba(15, 23, 42, 0.72), transparent);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.avatar-upload:not(.avatar-upload--readonly):hover .avatar-upload__hint {
  opacity: 1;
}

.basic-card__fields {
  flex: 1;
  min-width: 0;
}

.field-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px 40px;
}

.field-item {
  margin: 0;
}

.field-item__label {
  margin: 0 0 6px;
  font-size: var(--text-sm);
  color: var(--color-text-dim);
  font-weight: var(--font-medium);
}

.field-item__value {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  font-size: 14px;
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  word-break: break-all;
}

.field-item__value--mono {
  font-family: var(--font-family-mono);
  font-variant-numeric: tabular-nums;
  font-size: 13px;
}

.field-item__empty {
  color: var(--color-text-dim);
  font-weight: 400;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: var(--font-semibold);
  color: var(--profile-primary);
  background: var(--color-accent-soft);
}

.icon-action {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-dim);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.icon-action :deep(svg) {
  width: 14px;
  height: 14px;
}

.icon-action:hover {
  color: var(--profile-primary);
  background: var(--color-accent-soft);
}

.icon-action:active {
  transform: scale(0.96);
}

.icon-action:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

.text-link {
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  font-size: 13px;
  font-weight: var(--font-semibold);
  color: var(--profile-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.text-link:hover {
  color: var(--profile-primary-deep);
}

.text-link:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
  border-radius: 4px;
}

/* 安全设置列表 */
.security-list {
  margin: 0;
  padding: 0;
  list-style: none;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--profile-radius);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.security-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 28px;
  border-bottom: 1px solid var(--color-border-light);
}

.security-item:last-child {
  border-bottom: none;
}

.security-item__icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
}

.security-item__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.security-item__icon--primary {
  background: var(--color-accent-soft);
  color: var(--profile-primary);
}

.security-item__icon--email {
  background: var(--color-blue-soft);
  color: var(--color-blue);
}

.security-item__icon--phone {
  background: var(--color-green-soft);
  color: var(--color-green);
}

.security-item__icon--login {
  background: var(--color-accent-muted);
  color: var(--color-text-secondary);
}

.security-item__body {
  flex: 1;
  min-width: 0;
}

.security-item__title {
  margin: 0 0 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 15px;
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

.security-item__desc {
  margin: 0;
  font-size: var(--text-sm);
  line-height: 1.55;
  color: var(--color-text-secondary);
  max-width: 52ch;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: var(--font-semibold);
  letter-spacing: 0.02em;
}

.status-tag--ok {
  color: var(--color-green);
  background: var(--color-green-soft);
}

.status-tag--warn {
  color: #c0820a;
  background: rgba(192, 130, 10, 0.1);
}

.action-btn {
  flex-shrink: 0;
  height: 34px;
  padding: 0 18px;
  border-radius: var(--radius-control, 10px);
  border: 1px solid transparent;
  font-size: var(--text-base, 13px);
  font-weight: var(--font-semibold);
  font-family: inherit;
  cursor: pointer;
  transition: background var(--transition-normal), border-color var(--transition-normal), transform var(--transition-fast);
}

.action-btn--primary {
  background: var(--profile-primary);
  color: #fff;
}

.action-btn--primary:hover {
  background: var(--profile-primary-deep);
}

.action-btn:active {
  transform: scale(0.98);
}

.action-btn:focus-visible {
  outline: 2px solid var(--color-accent-glow);
  outline-offset: 2px;
}

/* 弹窗 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: var(--font-semibold);
  font-family: inherit;
  cursor: pointer;
  border: none;
  outline: none;
  transition: all var(--transition-normal);
}

.btn--primary {
  background: var(--profile-primary);
  color: #fff;
}

.btn--primary:hover {
  background: var(--profile-primary-deep);
}

.btn--outline {
  background: transparent;
  color: var(--color-text-body);
  border: 1px solid var(--color-border);
}

.btn--outline:hover {
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
}

.field-dialog :deep(.ant-modal-content),
.password-dialog :deep(.ant-modal-content) {
  border-radius: var(--profile-radius);
  overflow: hidden;
}

.field-dialog :deep(.ant-modal-header),
.password-dialog :deep(.ant-modal-header) {
  display: none;
}

.field-dialog :deep(.ant-modal-body),
.password-dialog :deep(.ant-modal-body) {
  padding: 0;
}

.field-dialog__header {
  padding: 28px 32px 0;
}

.field-dialog__header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.field-dialog__body {
  padding: 20px 32px 8px;
}

.field-dialog__body :deep(.ant-input-affix-wrapper),
.field-dialog__body :deep(.ant-input),
.field-dialog__body :deep(.ant-select-selector) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.field-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 32px 28px;
}

.password-dialog__header {
  text-align: center;
  padding: 36px 32px 12px;
}

.password-dialog__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  margin: 0 auto 14px;
  background: var(--color-accent-soft);
  color: var(--profile-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.password-dialog__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.password-dialog__header h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.password-dialog__header p {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-dim);
  line-height: 1.5;
}

.password-form {
  padding: 16px 32px 0;
}

.password-form :deep(.ant-form-item-label > label) {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-body);
  padding-bottom: 6px;
}

.password-form :deep(.ant-input-affix-wrapper), :deep(.ant-input) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.password-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 32px 28px;
}

@media (max-width: 960px) {
  .field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px 28px;
  }
}

@media (max-width: 720px) {
  .profile-page {
    padding: 16px 16px 32px;
  }

  .basic-card {
    flex-direction: column;
    align-items: center;
    padding: 24px 20px;
    gap: 24px;
  }

  .field-grid {
    width: 100%;
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .security-item {
    flex-wrap: wrap;
    padding: 18px 20px;
    gap: 14px;
  }

  .action-btn {
    width: 100%;
    margin-left: 62px;
  }
}
</style>
