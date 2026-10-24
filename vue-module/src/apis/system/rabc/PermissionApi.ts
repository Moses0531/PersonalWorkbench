import request from '@/utils/request'
import { normalizePermissionList } from '@/utils/menu'
import type { ApiResult, PageResult } from '@/types/api'
import type { RawSysPermission } from '@/types/entities'
import type { PermissionRecord, RawPermissionInput } from '@/types/menu'

const ALL_ROWS = 1000

/** 全量权限列表 */
export async function listPermissionsApi(): Promise<ApiResult<PermissionRecord[]>> {
  const res = await request.get<PageResult<RawSysPermission>>('/permission/getPermissionPage', {
    params: { pageNum: 1, pageRows: ALL_ROWS },
  })
  const list = normalizePermissionList(res.data?.records || [])
  return { ...res, data: list }
}

export function savePermissionApi(payload: RawPermissionInput): Promise<ApiResult> {
  return request.post('/permission/addPermission', payload)
}

export function updatePermissionApi(payload: RawPermissionInput): Promise<ApiResult> {
  return request.post('/permission/updatePermission', payload)
}

export function deletePermissionApi(permissionId: number | string): Promise<ApiResult> {
  return request.delete('/permission/deleteBatchPermission', {
    data: [Number(permissionId)],
  })
}
