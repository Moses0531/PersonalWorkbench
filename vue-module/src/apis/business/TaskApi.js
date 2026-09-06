import request from '@/utils/request'

const ALL_ROWS = 1000

/** 分页查询任务（可按 nodeType / parentTaskId / archived 过滤） */
export function pageTasksApi(pageNum = 1, pageRows = ALL_ROWS, filters = {}) {
  const { nodeType, parentTaskId, archived } = filters || {}
  return request.get('/task/getTaskPage', {
    params: {
      pageNum,
      pageRows: pageRows ?? ALL_ROWS,
      ...(nodeType != null && nodeType !== '' ? { nodeType } : {}),
      ...(parentTaskId != null && parentTaskId !== '' ? { parentTaskId } : {}),
      ...(archived != null && archived !== '' ? { archived } : {}),
    },
  })
}

/** 新增任务 / 阶段 / 顶层任务 */
export function addTaskApi(form) {
  return request.post('/task/addTask', form)
}

/** 更新任务 / 阶段 / 顶层任务 */
export function updateTaskApi(form) {
  return request.post('/task/updateTask', form)
}

/** 批量删除（级联子孙） */
export function deleteTasksApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/task/deleteBatchTask', { data: list })
}

/** 归档 / 恢复顶层任务 */
export function setProjectArchivedApi(projectTaskId, archived) {
  return request.post('/task/setProjectArchived', {
    projectTaskId: Number(projectTaskId),
    archived: String(archived),
  })
}

/** 上传任务附件（阿里云 OSS） */
export function uploadTaskAttachmentApi(taskId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/task/uploadAttachment', formData, {
    params: { taskId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除任务附件 */
export function removeTaskAttachmentApi(taskId, attachmentId) {
  return request.delete('/task/removeAttachment', {
    data: { taskId, attachmentId },
  })
}

/** 更新任务附件处理状态（0-未处理，1-已处理，2-处理完成） */
export function updateAttachmentStatusApi(taskId, attachmentId, status) {
  return request.post('/task/updateAttachmentStatus', {
    taskId,
    attachmentId,
    status: String(status),
  })
}

/** 任务组附件审查（汇总子孙任务附件；projectTaskId = 顶层任务 id） */
export function listProjectAttachmentsApi(projectTaskId) {
  return request.get('/task/listProjectAttachments', {
    params: { projectTaskId },
  })
}
