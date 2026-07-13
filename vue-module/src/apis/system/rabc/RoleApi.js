import request from '@/utils/request'
import { getCurrentUserProfileApi } from '@/apis/system/user/UserProfileApi'

const ALL_ROWS = 1000

/** 分页查询角色 */
export function pageRolesApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request.get('/role/getRolePage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 全量角色列表（包装分页接口） */
export async function listRolesApi() {
  const res = await pageRolesApi(1, ALL_ROWS)
  return { ...res, data: res.data?.records || [] }
}

/** 新增角色 */
export function saveRoleApi(form) {
  return request.post('/role/addRole', form)
}

/** 更新角色 */
export function updateRoleApi(form) {
  return request.post('/role/updateRole', form)
}

/** 删除角色 */
export function deleteRoleApi(roleId) {
  return request.delete('/role/deleteBatchRole', { data: [Number(roleId)] })
}

/** 受保护角色编码（ROOT/ADMIN 不可被非 ROOT 用户修改） */
export function getProtectedRoleCodesApi() {
  return Promise.resolve({ code: 1, data: ['ROOT', 'ADMIN'] })
}

/** 当前登录用户角色编码 */
export async function getCurrentUserRoleApi() {
  const [profileRes, rolesRes] = await Promise.all([
    getCurrentUserProfileApi(),
    listRolesApi(),
  ])
  const roleId = profileRes?.data?.roleId
  const roles = rolesRes?.data || []
  const role = roles.find((item) => Number(item.roleId) === Number(roleId))
  return { code: 1, data: role?.roleCode || '' }
}
