import request from '@/utils/request'

/** 用户登录 */
export function loginApi(payload) {
  return request.post('/auth/login', {
    account: payload.account,
    password: payload.password,
    captchaCode: payload.captchaCode,
    captchaToken: payload.captchaToken,
    captchaTimestamp: payload.captchaTimestamp,
  })
}

/** 用户注册 */
export function registerApi(payload) {
  return request.post('/auth/register', {
    phone: payload.phone,
    email: payload.email,
    password: payload.password,
    confirmPassword: payload.confirmPassword,
    captchaCode: payload.captchaCode,
    captchaToken: payload.captchaToken,
    captchaTimestamp: payload.captchaTimestamp,
  })
}
