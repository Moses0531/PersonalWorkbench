<template>
  <!-- 仅上传：任务附件 / 头像等场景复用 a-upload，不渲染编辑器 -->
  <div v-if="uploadOnly" class="rich-editor rich-editor--upload-only">
    <a-upload
        v-if="!readOnly"
        class="rich-editor__upload"
        :show-upload-list="false"
        :multiple="multiple"
        :accept="accept"
        :disabled="uploading"
        :before-upload="beforeUpload"
        :custom-request="handleCustomUpload"
    >
      <slot>
        <button type="button" class="toolbar-btn toolbar-btn--primary" :disabled="uploading">
          <UploadOutlined />
          {{ uploading ? '上传中...' : uploadText }}
        </button>
      </slot>
    </a-upload>
  </div>

  <div
      v-else
      class="rich-editor"
      :class="{ 'rich-editor--readonly': readOnly }"
      :style="containerStyle"
  >
    <div v-if="!readOnly" class="rich-editor__toolbar">
      <a-upload
          class="rich-editor__upload"
          :show-upload-list="false"
          :multiple="multiple"
          :accept="accept"
          :disabled="uploading || !editorReady"
          :before-upload="beforeUpload"
          :custom-request="handleCustomUpload"
      >
        <slot>
          <button
              type="button"
              class="toolbar-btn toolbar-btn--primary"
              :disabled="uploading || !editorReady"
          >
            <UploadOutlined />
            {{ uploading ? '上传中...' : uploadText }}
          </button>
        </slot>
      </a-upload>
      <div class="toolbar-divider"></div>
      <button type="button" class="toolbar-btn" :disabled="!editorReady" @click="handleUndo">
        <UndoOutlined />
        撤销
      </button>
      <button type="button" class="toolbar-btn" :disabled="!editorReady" @click="handleRedo">
        <RedoOutlined />
        重做
      </button>
    </div>
    <WangEditor
        v-if="!uploadOnly"
        class="rich-editor__body"
        :style="bodyStyle"
        :defaultConfig="editorConfig"
        mode="default"
        v-model="html"
        @onCreated="handleCreated"
    />
  </div>
</template>

<script setup>
/** 富文本编辑器（wangEditor），支持文件上传插入与只读模式；亦可 uploadOnly 仅作上传 */
import { computed, defineAsyncComponent, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, UndoOutlined, RedoOutlined } from '@ant-design/icons-vue'

/** 仅完整编辑模式才加载；uploadOnly 时不解析未安装/未使用的 wangEditor */
const WangEditor = defineAsyncComponent(async () => {
  await import('@wangeditor/editor/dist/css/style.css')
  const mod = await import('@wangeditor/editor-for-vue')
  return mod.Editor
})

const IMAGE_EXT = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp']
const VIDEO_EXT = ['.mp4', '.webm', '.ogg', '.mov', '.avi', '.mkv']

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  height: {
    type: Number,
    default: null,       // 固定高度时 body 区 = height - 工具栏
  },
  minHeight: {
    type: Number,
    default: 200,
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  fileSize: {
    type: Number,
    default: null,
  },
  placeholder: {
    type: String,
    default: '请输入内容',
  },
  /** 业务侧上传实现，必传（如任务附件 uploadTaskAttachmentApi） */
  uploadHandler: {
    type: Function,
    required: true,
  },
  resolveUrl: {
    type: Function,
    default: null,
  },
  /** 仅上传按钮（不渲染富文本），供任务附件等复用 */
  uploadOnly: {
    type: Boolean,
    default: false,
  },
  uploadText: {
    type: String,
    default: '上传文件',
  },
  accept: {
    type: String,
    default: '',
  },
  multiple: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()   // wangEditor 实例，需 shallowRef 避免深度响应
const html = ref('')
const uploading = ref(false)
const editorReady = ref(false)

const containerStyle = computed(() => {
  const style = {}
  if (props.height) {
    style.height = `${props.height}px`
  }
  return style
})

const bodyStyle = computed(() => {
  const style = {}
  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`
  }
  if (props.height) {
    style.height = `${props.height - 52}px`
  }
  return style
})

function defaultResolveUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path) || /^data:/i.test(path)) return path
  const normalized = String(path).trim()
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function resolveMediaUrl(path) {
  return (props.resolveUrl || defaultResolveUrl)(path)
}

function getFileExt(name = '') {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index).toLowerCase() : ''
}

function isImageFile(file) {
  if (file.type?.startsWith('image/')) return true
  return IMAGE_EXT.includes(getFileExt(file.name))
}

function isVideoFile(file) {
  if (file.type?.startsWith('video/')) return true
  return VIDEO_EXT.includes(getFileExt(file.name))
}

function escapeHtml(text) {
  return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
}

function validateFile(file) {
  if (props.accept?.includes('image/') && !isImageFile(file)) {
    message.error('只能上传图片文件')
    return false
  }
  if (props.fileSize != null && props.fileSize > 0 && file.size / 1024 / 1024 > props.fileSize) {
    message.error(`文件大小不能超过 ${props.fileSize} MB`)
    return false
  }
  return true
}

async function doUpload(file) {
  const result = await props.uploadHandler(file)
  if (result?.url) {
    return { ...result, url: resolveMediaUrl(result.url) }
  }
  return result || { url: '' }
}

/** 按 MIME/扩展名插入 img、video 或下载链接 */
function insertFileNode(editor, file, url) {
  editor.focus()

  if (isImageFile(file)) {
    editor.dangerouslyInsertHtml(`<p><img src="${escapeHtml(url)}" alt="${escapeHtml(file.name)}" /></p>`)
    return
  }

  if (isVideoFile(file)) {
    editor.dangerouslyInsertHtml(
        `<p><video src="${escapeHtml(url)}" controls style="max-width:100%"></video></p>`,
    )
    return
  }

  editor.dangerouslyInsertHtml(
      `<p><a href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(file.name)}</a></p>`,
  )
}

async function handleFileUpload(file) {
  if (!validateFile(file)) return false

  const result = await doUpload(file)
  const url = result?.url || ''

  if (!props.uploadOnly) {
    const editor = editorRef.value
    if (!editor) {
      message.warning('编辑器尚未就绪，请稍后再试')
      return false
    }
    if (url) insertFileNode(editor, file, url)
  }
  return true
}

/** a-upload：校验失败则拦截，避免进入 custom-request */
function beforeUpload(file) {
  if (!props.uploadOnly && !editorReady.value) {
    message.warning('编辑器尚未就绪，请稍后再试')
    return false
  }
  return validateFile(file)
}

/** a-upload custom-request */
async function handleCustomUpload(opts) {
  const file = opts?.file
  if (!file) {
    opts?.onError?.(new Error('未选择文件'))
    return
  }

  uploading.value = true
  try {
    await handleFileUpload(file)
    opts?.onSuccess?.({})
  } catch (err) {
    message.error(err.message || '文件上传失败')
    opts?.onError?.(err)
  } finally {
    uploading.value = false
  }
}

function handleUndo() {
  editorRef.value?.undo()
}

function handleRedo() {
  editorRef.value?.redo()
}

const editorConfig = {
  placeholder: props.placeholder,
  readOnly: props.readOnly,
  autoFocus: false,
  onChange(editor) {
    emit('update:modelValue', editor.getHtml())
  },
}

watch(
    () => props.placeholder,
    (value) => {
      editorConfig.placeholder = value
    },
)

watch(
    () => props.modelValue,
    (value) => {
      const next = value || ''
      if (next !== html.value) {
        html.value = next
      }
    },
    { immediate: true },
)

watch(html, (value) => {
  const next = value || ''
  if (next !== (props.modelValue || '')) {
    emit('update:modelValue', next)
  }
})

watch(
    () => props.readOnly,
    (value) => {
      const editor = editorRef.value
      if (!editor) return
      value ? editor.disable() : editor.enable()
    },
)

function handleCreated(editor) {
  editorRef.value = editor
  editorReady.value = true
  if (props.readOnly) {
    editor.disable()
  }
  editor.getEditableContainer().addEventListener('paste', handlePaste, true)
}

/** 拦截 Word 粘贴的 file:// 本地图片，引导用户走上传 */
async function handlePaste(event) {
  const clipboardData = event.clipboardData
  if (!clipboardData) return

  const items = clipboardData.items
  if (items) {
    for (const item of items) {
      if (!item.type.startsWith('image/')) continue
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        uploading.value = true
        try {
          await handleFileUpload(file)
        } catch (err) {
          message.error(err.message || '文件上传失败')
        } finally {
          uploading.value = false
        }
      }
      return
    }
  }

  const pastedHtml = clipboardData.getData('text/html')
  if (!pastedHtml || !/<img[\s>]/i.test(pastedHtml)) return

  if (/src=["']file:/i.test(pastedHtml)) {
    event.preventDefault()
    message.warning('无法直接使用 Word 等文档中的本地图片，请点击「上传文件」按钮上传')
  }
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped>
.rich-editor {
  --re-border: var(--color-border-strong);
  --re-border-hover: var(--color-accent-light);
  --re-surface: var(--color-surface);
  --re-surface-raised: var(--color-surface-muted);
  --re-toolbar-bg: var(--color-surface-muted);
  --re-toolbar-bg-subtle: linear-gradient(180deg, var(--color-surface-muted) 0%, var(--color-zone-content) 100%);
  --re-accent: var(--color-accent);
  --re-accent-hover: var(--color-accent-deep);
  --re-accent-soft: var(--color-accent-soft);
  --re-accent-glow: var(--color-accent-glow);
  --re-text: var(--color-text-primary);
  --re-text-muted: var(--color-text-secondary);
  --re-text-dim: var(--color-text-dim);
  --re-shadow-sm: var(--shadow-xs);
  --re-shadow-md: var(--shadow-sm);
  --re-shadow-lg: var(--shadow-md);
  --re-radius: 12px;
  --re-radius-sm: 8px;
  --re-transition: 0.22s cubic-bezier(0.25, 0.46, 0.45, 0.94);

  position: relative;
  border: 1px solid var(--re-border);
  border-radius: var(--re-radius);
  overflow: hidden;
  background: var(--re-surface);
  box-shadow: var(--re-shadow-md);
  transition: border-color var(--re-transition), box-shadow var(--re-transition);
  animation: editorFadeIn 0.5s ease both;
}

@keyframes editorFadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rich-editor:hover {
  border-color: var(--re-border-hover);
  box-shadow: var(--re-shadow-lg);
}

.rich-editor:focus-within {
  border-color: var(--re-accent);
  box-shadow: var(--re-shadow-lg), 0 0 0 3px var(--re-accent-glow);
}

/* ── Grain overlay ── */
.rich-editor::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.4'/%3E%3C/svg%3E");
  opacity: 0.018;
  pointer-events: none;
  z-index: 0;
  border-radius: inherit;
}

/* ── Toolbar ── */
.rich-editor__toolbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--re-border);
  background: var(--re-toolbar-bg);
  backdrop-filter: blur(8px);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--re-border);
  margin: 0 4px;
  opacity: 0.6;
}

.rich-editor__upload :deep(.ant-upload) {
  display: inline-block;
}

.rich-editor--upload-only {
  border: none;
  box-shadow: none;
  background: transparent;
  overflow: visible;
  animation: none;
}

.rich-editor--upload-only::before {
  display: none;
}

.rich-editor--upload-only:hover,
.rich-editor--upload-only:focus-within {
  border: none;
  box-shadow: none;
}

.toolbar-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: var(--re-radius-sm);
  background: transparent;
  color: var(--re-text-muted);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
      all var(--re-transition),
      transform 0.12s ease;
  white-space: nowrap;
  overflow: hidden;
}

.toolbar-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--re-accent-glow);
  opacity: 0;
  transition: opacity var(--re-transition);
  pointer-events: none;
}

.toolbar-btn:hover:not(:disabled)::after {
  opacity: 1;
}

.toolbar-btn:hover:not(:disabled) {
  color: var(--re-accent);
  border-color: var(--re-accent-glow);
  background: var(--re-accent-soft);
  transform: translateY(-1px);
}

.toolbar-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
}

.toolbar-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

.toolbar-btn--primary {
  background: var(--re-accent);
  color: #fff;
  border-color: var(--re-accent);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(192, 101, 42, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.toolbar-btn--primary::after {
  display: none;
}

.toolbar-btn--primary:hover:not(:disabled) {
  background: var(--re-accent-hover);
  border-color: var(--re-accent-hover);
  color: #fff;
  box-shadow: 0 3px 12px rgba(192, 101, 42, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.15);
  transform: translateY(-1px);
}

.toolbar-btn--primary:active:not(:disabled) {
  box-shadow: 0 1px 2px rgba(192, 101, 42, 0.2);
  transform: translateY(0) scale(0.97);
}

/* ── Editor body ── */
.rich-editor__body {
  position: relative;
  z-index: 1;
  overflow-y: auto;
  background: var(--re-surface);
}

/* ── Readonly state ── */
.rich-editor--readonly {
  border-style: solid;
  border-color: #ece8e1;
  box-shadow: var(--re-shadow-sm);
  background: #fdfcfa;
}

.rich-editor--readonly::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, transparent 50%, rgba(192, 101, 42, 0.04) 50%);
  pointer-events: none;
  z-index: 2;
}

/* ── WangEditor deep styles ── */
.rich-editor :deep(.w-e-text-container) {
  background: transparent !important;
}

.rich-editor :deep(.w-e-text-placeholder) {
  color: var(--re-text-dim) !important;
  font-style: italic !important;
  font-weight: 400 !important;
  letter-spacing: 0.01em;
  top: 16px !important;
  left: 20px !important;
}

.rich-editor :deep(.w-e-scroll) {
  min-height: inherit;
}

.rich-editor :deep(.w-e-text-container [data-slate-editor]) {
  padding: 16px 20px !important;
}

.rich-editor :deep(img),
.rich-editor :deep(video) {
  max-width: 100%;
  border-radius: var(--re-radius-sm);
  box-shadow: var(--re-shadow-md);
  transition: box-shadow var(--re-transition), transform var(--re-transition);
}

.rich-editor :deep(img:hover),
.rich-editor :deep(video:hover) {
  box-shadow: var(--re-shadow-lg);
  transform: scale(1.005);
}

.rich-editor :deep(a) {
  color: var(--re-accent);
  text-decoration: underline;
  text-decoration-color: rgba(192, 101, 42, 0.3);
  text-underline-offset: 3px;
  transition: text-decoration-color var(--re-transition);
}

.rich-editor :deep(a:hover) {
  text-decoration-color: var(--re-accent);
}

.rich-editor :deep(blockquote) {
  border-left: 3px solid var(--re-accent);
  padding-left: 16px;
  color: var(--re-text-muted);
  font-style: italic;
}

.rich-editor :deep(pre) {
  background: #f6f3ee;
  border: 1px solid var(--re-border);
  border-radius: var(--re-radius-sm);
  padding: 16px;
  font-size: 13px;
  overflow-x: auto;
}

.rich-editor :deep(code) {
  background: rgba(192, 101, 42, 0.08);
  color: var(--re-accent);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.rich-editor :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
  border-radius: 0;
}

.rich-editor :deep(h1),
.rich-editor :deep(h2),
.rich-editor :deep(h3) {
  color: var(--re-text);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.rich-editor :deep(hr) {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--re-border), var(--re-accent-glow), var(--re-border), transparent);
  margin: 20px 0;
}

/* ── Scrollbar ── */
.rich-editor__body::-webkit-scrollbar {
  width: 6px;
}

.rich-editor__body::-webkit-scrollbar-track {
  background: transparent;
}

.rich-editor__body::-webkit-scrollbar-thumb {
  background: rgba(140, 130, 121, 0.2);
  border-radius: 100px;
  transition: background 0.2s;
}

.rich-editor__body::-webkit-scrollbar-thumb:hover {
  background: rgba(140, 130, 121, 0.35);
}
</style>
