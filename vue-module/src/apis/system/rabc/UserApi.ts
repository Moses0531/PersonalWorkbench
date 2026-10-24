import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'
import type { UserForm, UserPayload, UserRow } from '@/types/entities'

const ALL_ROWS = 1000

function normalizeUser(row: UserRow): UserRow {
  if (!row) return row
  return {
    ...row,
    id: row.userId ?? row.id,
  }
}

function toUserPayload(form: UserForm): UserPayload {
  const payload: UserPayload = {
    userId: form.id ?? form.userId ?? null,
    account: form.account,
    username: form.username,
    realName: form.realName,
    phone: form.phone,
    email: form.email,
    sex: form.sex != null ? String(form.sex) : '0',
    roleId: form.roleId != null ? Number(form.roleId) : null,
  }
  if (form.password) payload.password = form.password
  return payload
}

/** 分页查询普通用户 */
export function pageUsersApi(
  pageNum = 1,
  pageRows: number | null = ALL_ROWS,
): Promise<ApiResult<PageResult<UserRow>>> {
  return request
    .get<PageResult<UserRow>>('/user/getPageUser', { params: { pageNum, pageRows: pageRows ?? ALL_ROWS } })
    .then((res) => ({
      ...res,
      data: {
        ...res.data!,
        records: (res.data?.records || []).map(normalizeUser),
      },
    }))
}

/** 分页查询管理员 */
export function pageAdminsApi(
  pageNum = 1,
  pageRows: number | null = ALL_ROWS,
): Promise<ApiResult<PageResult<UserRow>>> {
  return request
    .get<PageResult<UserRow>>('/user/getPageAdmin', { params: { pageNum, pageRows: pageRows ?? ALL_ROWS } })
    .then((res) => ({
      ...res,
      data: {
        ...res.data!,
        records: (res.data?.records || []).map(normalizeUser),
      },
    }))
}

/** 新增用户/管理员 */
export function saveUserApi(form: UserForm): Promise<ApiResult> {
  return request.post('/user/insertUser', toUserPayload(form))
}

/** 更新用户/管理员 */
export function updateUserApi(form: UserForm): Promise<ApiResult> {
  return request.post('/user/updateUser', toUserPayload(form))
}

/** 删除用户/管理员 */
export function deleteUserApi(id: number | string): Promise<ApiResult> {
  return request.delete('/user/deleteBatchUser', { data: [Number(id)] })
}
