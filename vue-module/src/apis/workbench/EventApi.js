import request from '@/utils/request'

const ALL_ROWS = 1000

/** 分页查询日程 */
export function pageEventsApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request.get('/wbEvent/getEventPage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 新增日程 */
export function addEventApi(form) {
  return request.post('/wbEvent/addEvent', form)
}

/** 更新日程 */
export function updateEventApi(form) {
  return request.post('/wbEvent/updateEvent', form)
}

/** 批量删除日程 */
export function deleteEventsApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/wbEvent/deleteBatchEvent', { data: list })
}
