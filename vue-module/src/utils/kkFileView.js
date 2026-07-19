/**
 * kkFileView 在线预览
 * - 本地：VITE_KK_FILE_VIEW_BASE=http://127.0.0.1:8012（.env.development）
 * - 服务器：同域 /preview（.env.production + nginx → kkfileview）
 */
function utf8ToBase64(str) {
  const bytes = new TextEncoder().encode(str)
  let binary = ''
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

export function getKkFileViewBase() {
  return String(import.meta.env.VITE_KK_FILE_VIEW_BASE || 'http://127.0.0.1:8012').replace(/\/$/, '')
}

/** 用 kkFileView 打开文件预览（新标签页） */
export function openKkFileViewPreview(fileUrl) {
  if (!fileUrl) return
  const base = getKkFileViewBase()
  const encoded = encodeURIComponent(utf8ToBase64(fileUrl))
  window.open(`${base}/onlinePreview?url=${encoded}`, '_blank', 'noopener,noreferrer')
}
