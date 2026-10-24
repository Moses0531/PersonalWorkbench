import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { AuthConfig, LoginPayload, RegisterPayload, RawSysPermission, SessionInfo } from '@/types/entities'

/** 获取认证公开配置（注册/验证码开关） */
export function fetchAuthConfigApi(): Promise<ApiResult<AuthConfig>> {
  return request.get<AuthConfig>('/auth/config')
}

/** 用户登录：account 支持系统账号、手机号或邮箱 */
export function loginApi(payload: LoginPayload): Promise<ApiResult<SessionInfo>> {
  return request.post<SessionInfo>('/auth/login', {
    account: payload.account,
    password: payload.password,
    captchaCode: payload.captchaCode,
    captchaToken: payload.captchaToken,
    captchaTimestamp: payload.captchaTimestamp,
  })
}

/** 获取当前登录用户的菜单与功能权限（实时从数据库加载） */
export function fetchUserMenusApi(): Promise<ApiResult<RawSysPermission[]>> {
  return request.get<RawSysPermission[]>('/auth/menus')
}

/** 用户注册并自动登录：phone 与 email 二选一，成功后返回会话信息 */
export function registerApi(payload: RegisterPayload): Promise<ApiResult<SessionInfo>> {
  const body: RegisterPayload = {
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
  return request.post<SessionInfo>('/auth/register', body)
}
