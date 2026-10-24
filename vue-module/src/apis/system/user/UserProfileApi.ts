import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { ChangePasswordPayload, SysUser, UpdateProfilePayload } from '@/types/entities'

/** 获取当前登录用户资料 */
export function getCurrentUserProfileApi(): Promise<ApiResult<SysUser>> {
  return request.get<SysUser>('/user-profile/getCurrentUserProfile')
}

/** 更新个人信息（不含头像，头像走 uploadAvatarApi） */
export function updateUserProfileApi(payload: UpdateProfilePayload): Promise<ApiResult> {
  return request.put('/user-profile/updateUserProfile', payload)
}

/** 修改登录密码 */
export function changePasswordApi(payload: ChangePasswordPayload): Promise<ApiResult> {
  return request.post('/user-profile/changePassword', payload)
}

/** 上传头像 */
export function uploadAvatarApi(file: File): Promise<ApiResult<string>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/user-profile/updateAvatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
