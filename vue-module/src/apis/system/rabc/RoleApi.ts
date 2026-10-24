import request from '@/utils/request'
import { getCurrentUserProfileApi } from '@/apis/system/user/UserProfileApi'
import type { ApiResult, PageResult } from '@/types/api'
import type { SysRole } from '@/types/entities'

const ALL_ROWS = 1000

/** 分页查询角色 */
export function pageRolesApi(
  pageNum = 1,
  pageRows: number | null = ALL_ROWS,
): Promise<ApiResult<PageResult<SysRole>>> {
  return request.get<PageResult<SysRole>>('/role/getRolePage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 全量角色列表（包装分页接口） */
export async function listRolesApi(): Promise<ApiResult<SysRole[]>> {
  const res = await pageRolesApi(1, ALL_ROWS)
  return { ...res, data: res.data?.records || [] }
}

/** 新增角色 */
export function saveRoleApi(form: SysRole): Promise<ApiResult> {
  return request.post('/role/addRole', form)
}

/** 更新角色 */
export function updateRoleApi(form: SysRole): Promise<ApiResult> {
  return request.post('/role/updateRole', form)
}

/** 删除角色 */
export function deleteRoleApi(roleId: number | string): Promise<ApiResult> {
  return request.delete('/role/deleteBatchRole', { data: [Number(roleId)] })
}

/** 受保护角色编码（ROOT/ADMIN 不可被非 ROOT 用户修改） */
export function getProtectedRoleCodesApi(): Promise<ApiResult<string[]>> {
  return Promise.resolve({ code: 1, msg: 'ok', data: ['ROOT', 'ADMIN'] })
}

/** 当前登录用户角色编码 */
export async function getCurrentUserRoleApi(): Promise<ApiResult<string>> {
  const [profileRes, rolesRes] = await Promise.all([
    getCurrentUserProfileApi(),
    listRolesApi(),
  ])
  const roleId = profileRes?.data?.roleId
  const roles = rolesRes?.data || []
  const role = roles.find((item) => Number(item.roleId) === Number(roleId))
  return { code: 1, msg: 'ok', data: role?.roleCode || '' }
}
