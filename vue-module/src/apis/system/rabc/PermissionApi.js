import request from '@/utils/request'
import { componentPathFromRouterName } from '@/utils/menu'

const ALL_ROWS = 1000

function coalescePermissionFields(permission) {
  let routerName = String(permission.routerName ?? permission.router_name ?? '').trim()
  let componentPath = String(permission.componentPath ?? permission.component_path ?? '').trim()

  if (routerName.startsWith('/') && componentPath && !componentPath.startsWith('/')) {
    return { routerName: componentPath, componentPath: routerName }
  }
  return { routerName, componentPath }
}

/** 后端 SysPermission → 前端统一字段 */
export function normalizePermissionList(list = []) {
  return list.map((item) => {
    const { routerName, componentPath } = coalescePermissionFields(item)
    const resolvedPath =
      componentPath ||
      (routerName && !routerName.startsWith('/') ? componentPathFromRouterName(routerName) : routerName) ||
      ''

    return {
      ...item,
      permissionId: item.permissionId ?? item.permission_id,
      parentId: item.parentId ?? item.parent_id ?? 0,
      routerName: routerName.replace(/\.vue$/i, ''),
      componentPath: resolvedPath,
      isDisplay: item.isDisplay ?? item.is_display ?? 0,
      type: String(item.type || 'D').trim().toUpperCase(),
    }
  })
}

export function permissionTypeOf(row) {
  return String(row?.type || '').trim().toUpperCase()
}

export function componentPathOf(row) {
  if (!row) return ''
  const path = row.componentPath ?? row.component_path ?? ''
  if (path) return path
  const routerName = row.routerName ?? row.router_name ?? ''
  return componentPathFromRouterName(routerName) || ''
}

export function isDisplayOf(row) {
  const val = row?.isDisplay ?? row?.is_display
  return val == null ? 0 : Number(val)
}

export function viewFileFromRouterName(routerName) {
  const name = String(routerName ?? '').trim().replace(/\.vue$/i, '')
  if (!name) return ''
  return `views/**/${name}.vue`
}

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
