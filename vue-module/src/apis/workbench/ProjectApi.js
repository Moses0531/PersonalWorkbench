import request from '@/utils/request'

const ALL_ROWS = 1000

/** 分页查询项目 */
export function pageProjectsApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request.get('/wbProject/getProjectPage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 新增项目 */
export function addProjectApi(form) {
  return request.post('/wbProject/addProject', form)
}

/** 更新项目 */
export function updateProjectApi(form) {
  return request.post('/wbProject/updateProject', form)
}

/** 批量删除项目 */
export function deleteProjectsApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/wbProject/deleteBatchProject', { data: list })
}
