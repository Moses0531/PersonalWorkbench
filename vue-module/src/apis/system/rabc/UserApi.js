import request from '@/utils/request'

const ALL_ROWS = 1000

function normalizeUser(row) {
  if (!row) return row
  return {
    ...row,
    id: row.userId ?? row.id,
  }
}

function toUserPayload(form) {
  const payload = {
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
export function pageUsersApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request
    .get('/user/getPageUser', { params: { pageNum, pageRows: pageRows ?? ALL_ROWS } })
    .then((res) => ({
      ...res,
      data: {
        ...res.data,
        records: (res.data?.records || []).map(normalizeUser),
      },
    }))
}

/** 分页查询管理员 */
export function pageAdminsApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request
    .get('/user/getPageAdmin', { params: { pageNum, pageRows: pageRows ?? ALL_ROWS } })
    .then((res) => ({
      ...res,
      data: {
        ...res.data,
        records: (res.data?.records || []).map(normalizeUser),
      },
    }))
}

/** 新增用户/管理员 */
export function saveUserApi(form) {
  return request.post('/user/insertUser', toUserPayload(form))
}

/** 更新用户/管理员 */
export function updateUserApi(form) {
  return request.post('/user/updateUser', toUserPayload(form))
}

/** 删除用户/管理员 */
export function deleteUserApi(id) {
  return request.delete('/user/deleteBatchUser', { data: [Number(id)] })
}
