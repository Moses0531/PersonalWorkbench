import request from '@/utils/request'

const ALL_ROWS = 1000

/** 分页查询任务 */
export function pageTasksApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request.get('/wbTask/getTaskPage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 新增任务 */
export function addTaskApi(form) {
  return request.post('/wbTask/addTask', form)
}

/** 更新任务 */
export function updateTaskApi(form) {
  return request.post('/wbTask/updateTask', form)
}

/** 批量删除任务 */
export function deleteTasksApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/wbTask/deleteBatchTask', { data: list })
}
