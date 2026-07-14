import request from '@/utils/request'

/** 获取认证公开配置（注册/验证码开关） */
export function fetchAuthConfigApi() {
  return request.get('/auth/config')
}

/** 用户登录：account 支持系统账号、手机号或邮箱 */
export function loginApi(payload) {
  return request.post('/auth/login', {
    account: payload.account,
    password: payload.password,
    captchaCode: payload.captchaCode,
    captchaToken: payload.captchaToken,
    captchaTimestamp: payload.captchaTimestamp,
  })
}

/** 获取当前登录用户的菜单与功能权限（实时从数据库加载） */
export function fetchUserMenusApi() {
  return request.get('/auth/menus')
}

/** 用户注册：phone 与 email 二选一 */
export function registerApi(payload) {
  const body = {
    password: payload.password,
    confirmPassword: payload.confirmPassword,
    captchaCode: payload.captchaCode,
    captchaToken: payload.captchaToken,
    captchaTimestamp: payload.captchaTimestamp,
  }
  if (payload.phone) {
    body.phone = payload.phone
  }
  if (payload.email) {
    body.email = payload.email
  }
  return request.post('/auth/register', body)
}

