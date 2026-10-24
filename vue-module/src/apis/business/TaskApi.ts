import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'
import type { IdLike } from '@/types/entities'

const ALL_ROWS = 1000

/** 任务附件记录（附件 JSON 字段兼容 id/url/handled 等别名） */
export interface TaskAttachmentRecord {
  id?: IdLike
  attachmentId?: IdLike
  taskId?: IdLike
  name?: string
  taskTitle?: string
  fileName?: string
  fileUrl?: string
  url?: string
  size?: string | number
  fileSize?: string | number
  status?: string | number
  handled?: unknown
  createTime?: string | null
  [key: string]: unknown
}

/** 任务记录（task 表前端投影；不同 nodeType 字段有差异，故均可选） */
export interface TaskRecord {
  taskId?: IdLike | null
  parentTaskId?: IdLike | null
  nodeType?: string | null
  node_type?: string | null
  title?: string | null
  description?: string | null
  status?: string | number | null
  priority?: string | number | null
  startTime?: string | null
  dueTime?: string | null
  createTime?: string | null
  updateTime?: string | null
  displayOrder?: string | number | null
  archived?: string | boolean | null
  tags?: string | null
  remark?: string | null
  attachments?: TaskAttachmentRecord[]
  children?: TaskRecord[]
  [key: string]: unknown
}

/** 任务分页查询过滤条件 */
export interface TaskQueryFilters {
  nodeType?: IdLike | null
  parentTaskId?: IdLike | null
  archived?: string | boolean | null
}

/** 分页查询任务（可按 nodeType / parentTaskId / archived 过滤） */
export function pageTasksApi(
  pageNum = 1,
  pageRows: number | null = ALL_ROWS,
  filters: TaskQueryFilters = {},
): Promise<ApiResult<PageResult<TaskRecord>>> {
  const { nodeType, parentTaskId, archived } = filters
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
export function addTaskApi(form: Record<string, unknown>) {
  return request.post('/task/addTask', form)
}

/** 更新任务 / 阶段 / 顶层任务 */
export function updateTaskApi(form: Record<string, unknown>) {
  return request.post('/task/updateTask', form)
}

/** 批量删除（级联子孙） */
export function deleteTasksApi(ids: IdLike | IdLike[]) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/task/deleteBatchTask', { data: list })
}

/** 归档 / 恢复顶层任务 */
export function setProjectArchivedApi(projectTaskId: IdLike, archived: boolean | string) {
  return request.post('/task/setProjectArchived', {
    projectTaskId: Number(projectTaskId),
    archived: String(archived),
  })
}

/** 上传任务附件（阿里云 OSS） */
export function uploadTaskAttachmentApi(taskId: IdLike, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/task/uploadAttachment', formData, {
    params: { taskId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除任务附件 */
export function removeTaskAttachmentApi(taskId: IdLike, attachmentId: IdLike) {
  return request.delete('/task/removeAttachment', {
    data: { taskId, attachmentId },
  })
}

/** 更新任务附件处理状态（0-未处理，1-已处理，2-处理完成） */
export function updateAttachmentStatusApi(taskId: IdLike, attachmentId: IdLike, status: number | string) {
  return request.post('/task/updateAttachmentStatus', {
    taskId,
    attachmentId,
    status: String(status),
  })
}

/** 任务组附件审查（汇总子孙任务附件；projectTaskId = 顶层任务 id） */
export function listProjectAttachmentsApi(
  projectTaskId: IdLike,
): Promise<ApiResult<TaskAttachmentRecord[]>> {
  return request.get('/task/listProjectAttachments', {
    params: { projectTaskId },
  })
}
