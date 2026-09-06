import request from '@/utils/request'

const ALL_ROWS = 200

/** 分页查询笔记（支持 keyword / noteType / status） */
export function pageNotesApi(params = {}) {
  const {
    pageNum = 1,
    pageRows = ALL_ROWS,
    keyword,
    noteType,
    status,
  } = params
  return request.get('/note/getNotePage', {
    params: {
      pageNum,
      pageRows: pageRows ?? ALL_ROWS,
      keyword: keyword || undefined,
      noteType: noteType || undefined,
      status: status || undefined,
    },
  })
}

/** 获取单条笔记 */
export function getNoteApi(noteId) {
  return request.get('/note/getNote', { params: { noteId } })
}

/** 新增笔记 */
export function addNoteApi(form) {
  return request.post('/note/addNote', form)
}

/** 更新笔记 */
export function updateNoteApi(form) {
  return request.post('/note/updateNote', form)
}

/** 批量删除笔记 */
export function deleteNotesApi(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map(Number)
  return request.delete('/note/deleteBatchNote', { data: list })
}

/** 归档 / 取消归档 */
export function archiveNoteApi(noteId, status = '1') {
  return request.post('/note/archiveNote', { noteId, status })
}
