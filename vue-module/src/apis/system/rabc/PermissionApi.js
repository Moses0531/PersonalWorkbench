import request from '@/utils/request'
import { normalizePermissionList } from '@/utils/menu'

const ALL_ROWS = 1000

/** 全量权限列表 */
export async function listPermissionsApi() {
  const res = await request.get('/permission/getPermissionPage', {
    params: { pageNum: 1, pageRows: ALL_ROWS },
  })
  const list = normalizePermissionList(res.data?.records || [])
  return { ...res, data: list }
}

export function savePermissionApi(payload) {
  return request.post('/permission/addPermission', payload)
}

export function updatePermissionApi(payload) {
  return request.post('/permission/updatePermission', payload)
}

export function deletePermissionApi(permissionId) {
  return request.delete('/permission/deleteBatchPermission', {
    data: [Number(permissionId)],
  })
}
