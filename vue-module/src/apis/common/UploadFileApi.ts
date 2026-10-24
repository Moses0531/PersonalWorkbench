/** 通用文件上传接口（对应 common-module 的文件上传能力，参数名 file） */
import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

/** 上传文件，返回可访问的文件地址 */
export function uploadFileApi(file: File): Promise<ApiResult<string>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
