import request from '@/utils/request'

/** 获取 Base64 验证码 */
export function fetchCaptchaBase64Api() {
  return request.get('/captchas/getCaptchaBase64')
}
