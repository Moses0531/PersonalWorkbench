import request from '@/utils/request'

/** 获取当前登录用户资料 */
export function getCurrentUserProfileApi() {
  return request.get('/user-profile/getCurrentUserProfile')
}

/** 更新个人信息（不含头像，头像走 uploadAvatarApi） */
export function updateUserProfileApi(payload) {
  return request.put('/user-profile/updateUserProfile', payload)
}

/** 修改登录密码 */
export function changePasswordApi(payload) {
  return request.post('/user-profile/changePassword', payload)
}

/** 上传头像 */
export function uploadAvatarApi(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user-profile/updateAvatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
