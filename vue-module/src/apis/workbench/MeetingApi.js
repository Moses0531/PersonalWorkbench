import request from '@/utils/request'

const ALL_ROWS = 1000

/** 分页查询会议 */
export function pageMeetingsApi(pageNum = 1, pageRows = ALL_ROWS) {
  return request.get('/wbMeeting/getMeetingPage', {
    params: { pageNum, pageRows: pageRows ?? ALL_ROWS },
  })
}

/** 新增会议（返回含 meetingId 的记录） */
export function addMeetingApi(form) {
  return request.post('/wbMeeting/addMeeting', form)
}

/** 更新会议 */
export function updateMeetingApi(form) {
  return request.post('/wbMeeting/updateMeeting', form)
}

/** 批量删除会议 */
export function deleteMeetingsApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/wbMeeting/deleteBatchMeeting', { data: list })
}

/** 上传会议材料 */
export function uploadMeetingAttachmentApi(meetingId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/wbMeeting/uploadAttachment', formData, {
    params: { meetingId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除会议材料 */
export function removeMeetingAttachmentApi(meetingId, attachmentId) {
  return request.delete('/wbMeeting/removeAttachment', {
    data: { meetingId, attachmentId },
  })
}
