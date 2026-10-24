import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { CaptchaData } from '@/types/entities'

/** 获取 Base64 验证码 */
export function fetchCaptchaBase64Api(): Promise<ApiResult<CaptchaData>> {
  return request.get<CaptchaData>('/captchas/getCaptchaBase64')
}
