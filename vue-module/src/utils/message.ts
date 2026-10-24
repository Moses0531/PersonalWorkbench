import { ElMessage, ElNotification, ElMessageBox, ElLoading } from 'element-plus'
import type { ElMessageBoxOptions, MessageOptions, NotificationOptions } from 'element-plus'

const message = {
  success: (msg: string, options?: Partial<MessageOptions>) => ElMessage.success({ message: msg, ...options }),
  warning: (msg: string, options?: Partial<MessageOptions>) => ElMessage.warning({ message: msg, ...options }),
  error: (msg: string, options?: Partial<MessageOptions>) => ElMessage.error({ message: msg, ...options }),
  info: (msg: string, options?: Partial<MessageOptions>) => ElMessage.info({ message: msg, ...options }),
}

const notification = {
  success: (options?: NotificationOptions) => ElNotification.success(options),
  warning: (options?: NotificationOptions) => ElNotification.warning(options),
  error: (options?: NotificationOptions) => ElNotification.error(options),
  info: (options?: NotificationOptions) => ElNotification.info(options),
}

/** 对话框选项：content/message 二选一，positiveText/negativeText 映射到按钮文案 */
export interface DialogOptions extends Partial<ElMessageBoxOptions> {
  content?: string
  message?: string
  title?: string
  positiveText?: string
  negativeText?: string
}

const dialog = {
  warning: (options: DialogOptions) => {
    const { content, message: text, title, positiveText, negativeText, ...rest } = options
    return ElMessageBox.confirm(
      String(content || text || ''),
      String(title || '提示'),
      {
        type: 'warning',
        confirmButtonText: String(positiveText || '确定'),
        cancelButtonText: String(negativeText || '取消'),
        ...rest,
      },
    )
  },
  info: (options: DialogOptions) => {
    const { content, message: text, title, positiveText, negativeText, ...rest } = options
    return ElMessageBox.alert(
      String(content || text || ''),
      String(title || '提示'),
      {
        type: 'info',
        confirmButtonText: String(positiveText || '确定'),
        ...rest,
      },
    )
  },
}

let loadingInstance: ReturnType<typeof ElLoading.service> | null = null
const loadingBar = {
  start() {
    loadingInstance?.close()
    loadingInstance = ElLoading.service({
      fullscreen: true,
      lock: true,
      background: 'rgba(240, 247, 251, 0.35)',
    })
  },
  finish() {
    loadingInstance?.close()
    loadingInstance = null
  },
  error() {
    loadingInstance?.close()
    loadingInstance = null
  },
}

export { message, notification, dialog, loadingBar }
