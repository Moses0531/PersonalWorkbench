import request from '@/utils/request'
import { listRolesApi } from './RoleApi'

/** 查询角色已绑定的权限 ID 列表 */
export async function listRolePermissionIdsApi(roleId) {
  const res = await request.get('/rolePermission/getPermissionsByRoleId', {
    params: { roleId },
  })
  const ids = (res.data || []).map((item) => Number(item.permissionId))
  return { ...res, data: ids }
}

/** 批量保存角色权限（客户端 diff 增删） */
export async function saveRolePermissionsApi(roleId, permissionIds = []) {
  const currentRes = await listRolePermissionIdsApi(roleId)
  const currentSet = new Set((currentRes.data || []).map(Number))
  const nextSet = new Set(permissionIds.map(Number))

  const toAdd = [...nextSet].filter((id) => !currentSet.has(id))
  const toRemove = [...currentSet].filter((id) => !nextSet.has(id))

  await Promise.all([
    ...toAdd.map((permissionId) =>
      request.post('/rolePermission/addRolePermission', {
        roleId: Number(roleId),
        permissionId,
      }),
    ),
    ...toRemove.map((permissionId) =>
      request.delete('/rolePermission/removeRolePermission', {
        data: { roleId: Number(roleId), permissionId },
      }),
    ),
  ])
  return { code: 1 }
}

/** 查询拥有某权限的角色 ID 列表 */
export async function listPermissionRoleIdsApi(permissionId) {
  const rolesRes = await listRolesApi()
  const roles = rolesRes.data || []
  const roleIds = []

  for (const role of roles) {
    const res = await listRolePermissionIdsApi(role.roleId)
    if ((res.data || []).includes(Number(permissionId))) {
      roleIds.push(Number(role.roleId))
    }
  }

  return { code: 1, data: roleIds }
}

/** 批量分配权限给角色（客户端 diff 增删） */
export async function assignPermissionRolesApi(permissionId, roleIds = []) {
  const currentRes = await listPermissionRoleIdsApi(permissionId)
  const currentSet = new Set((currentRes.data || []).map(Number))
  const nextSet = new Set(roleIds.map(Number))

  const toAdd = [...nextSet].filter((id) => !currentSet.has(id))
  const toRemove = [...currentSet].filter((id) => !nextSet.has(id))

  await Promise.all([
    ...toAdd.map((roleId) =>
      request.post('/rolePermission/addRolePermission', {
        roleId,
        permissionId: Number(permissionId),
      }),
    ),
    ...toRemove.map((roleId) =>
      request.delete('/rolePermission/removeRolePermission', {
        data: { roleId, permissionId: Number(permissionId) },
      }),
    ),
  ])
  return { code: 1 }
}
