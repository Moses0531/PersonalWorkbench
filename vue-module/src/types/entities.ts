/** 后端实体与接口出入参结构（字段与 SysUser/SysRole/SysPermission 等 Java 实体对应） */

/** 后端 ID 字段（Long 序列化后可能是数字或字符串） */
export type IdLike = number | string

/** 系统用户（对应 SysUser） */
export interface SysUser {
  userId?: number
  account?: string
  password?: string
  status?: string
  roleId?: number
  username?: string
  phone?: string
  email?: string
  realName?: string
  sex?: string
  avatar?: string
  birthday?: string
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

/** 登录态中的用户简要信息（userStore.user） */
export interface SysUserBrief {
  userId?: number
  account?: string
  username?: string
  avatar?: string
  roleId?: number
}

/** 管理页用户行（normalizeUser 输出，统一以 id 为主键） */
export interface UserRow extends SysUser {
  id?: IdLike
}

/** 新增/更新用户的请求体（toUserPayload 输出） */
export interface UserPayload {
  userId?: IdLike | null
  account?: string
  username?: string
  realName?: string
  phone?: string
  email?: string
  sex?: string
  roleId?: number | null
  password?: string
}

/** 管理页用户表单（字段可能来自表单控件，类型较宽） */
export interface UserForm {
  id?: IdLike | null
  userId?: number
  account?: string
  username?: string
  realName?: string
  phone?: string
  email?: string
  sex?: string | number
  roleId?: number | string | null
  password?: string
}

/** 系统角色（对应 SysRole） */
export interface SysRole {
  roleId?: number | null
  roleName?: string
  roleCode?: string
  level?: number | null
  description?: string
  status?: string
  createTime?: string
  updateTime?: string
}

/** 权限（对应 SysPermission） */
export interface SysPermission {
  permissionId?: IdLike
  name?: string
  parentId?: IdLike
  type?: string
  status?: string
  code?: string
  icon?: string
  remark?: string
  routerName?: string
  displayOrder?: number | string
  componentPath?: string
  isDisplay?: number | string
  createTime?: string
  updateTime?: string
}

/**
 * 权限原始结构（宽松输入）。
 * 兼容旧序列化的 snake_case 字段，normalize 系列函数统一收敛为 camelCase。
 */
export interface RawSysPermission extends Partial<SysPermission> {
  permission_id?: IdLike
  parent_id?: IdLike
  router_name?: string
  component_path?: string
  is_display?: number | string
  display_order?: number | string
  [key: string]: unknown
}

/** 角色-权限关联（对应 RolePermission） */
export interface RolePermission {
  id?: number
  roleId?: number
  permissionId?: number
}

/** AI 会话元信息（对应 AIConversationMeta） */
export interface AIConversationMeta {
  conversationId: string
  title: string
  lastActiveAt?: string
}

/** AI 对话消息 */
export interface AiChatMessage {
  role: 'user' | 'assistant' | string
  content: string
  timestamp?: string
}

/** 验证码（Base64 图 + 令牌 + 时间戳） */
export interface CaptchaData {
  img: string
  token: string
  timestamp: number
}

/** 认证公开配置（注册/验证码开关） */
export interface AuthConfig {
  registerEnabled?: boolean
  captchaOnLogin?: boolean
  captchaOnRegister?: boolean
}

/** 登录/注册成功后的会话信息 */
export interface SessionInfo {
  token: string
  userId: number
  account?: string
  username?: string
  avatar?: string
  roleId?: number
  menuList?: RawSysPermission[]
}

/** 登录请求体 */
export interface LoginPayload {
  account: string
  password: string
  captchaCode?: string
  captchaToken?: string
  captchaTimestamp?: number
}

/** 注册请求体（phone 与 email 二选一） */
export interface RegisterPayload {
  password: string
  confirmPassword: string
  captchaCode?: string
  captchaToken?: string
  captchaTimestamp?: number
  phone?: string
  email?: string
}

/** 个人资料更新请求体 */
export interface UpdateProfilePayload {
  userId?: number | null
  username?: string
  realName?: string
  phone?: string
  email?: string
  sex?: string
  birthday?: string | null
}

/** 修改密码请求体 */
export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
  confirmPassword?: string
}
