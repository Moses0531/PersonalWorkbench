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

/** 上传任务附件（阿里云 OSS） */
export function uploadTaskAttachmentApi(taskId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/wbTask/uploadAttachment', formData, {
    params: { taskId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除任务附件 */
export function removeTaskAttachmentApi(taskId, attachmentId) {
  return request.delete('/wbTask/removeAttachment', {
    data: { taskId, attachmentId },
  })
}

/** 项目附件审查（汇总下属任务附件） */
export function listProjectAttachmentsApi(projectId) {
  return request.get('/wbTask/listProjectAttachments', {
    params: { projectId },
  })
}

/** AI 规划预览（不写库） */
export function planPreviewApi(body) {
  return request.post('/ai/plan/preview', body)
}

/** AI 规划确认落板 */
export function planApplyApi(body) {
  return request.post('/ai/plan/apply', body)
}

