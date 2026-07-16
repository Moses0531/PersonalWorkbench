<script setup>
import { computed } from 'vue'

/** 管理页新增/编辑弹窗外壳：统一宽度、双列栅格、确认/取消 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  /** 显式宽度；不传时 1 列 520 / 2 列 960 */
  width: { type: [String, Number], default: undefined },
  /** 1 单列 · 2 一行两字段（会给内容区加上 dialog-grid 双列能力） */
  columns: { type: Number, default: 1 },
  loading: { type: Boolean, default: false },
  confirmText: { type: String, default: '保存' },
  cancelText: { type: String, default: '取消' },
  closeOnClickModal: { type: Boolean, default: false },
  destroyOnClose: { type: Boolean, default: false },
  centered: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const isTwoCol = computed(() => Number(props.columns) === 2)

const resolvedWidth = computed(() => {
  if (props.width !== undefined && props.width !== null && props.width !== '') {
    return props.width
  }
  return isTwoCol.value ? 960 : 520
})

const wrapClassName = computed(() =>
  ['form-dialog', 'data-operation-view', isTwoCol.value ? 'is-cols-2' : 'is-cols-1']
    .join(' '),
)

function close() {
  emit('update:modelValue', false)
}

function onCancel() {
  close()
  emit('cancel')
}

function onConfirm() {
  emit('confirm')
}

function onOpenChange(open) {
  emit('update:modelValue', open)
}
</script>

<template>
  <a-modal
    :open="modelValue"
    :title="title"
    :width="resolvedWidth"
    :wrap-class-name="wrapClassName"
    :centered="centered"
    :mask-closable="closeOnClickModal"
    :destroy-on-close="destroyOnClose"
    :footer="null"
    @update:open="onOpenChange"
  >
    <div
      class="dop-body"
      :class="isTwoCol ? 'dop-body--2' : 'dop-body--1'"
      :data-cols="isTwoCol ? 2 : 1"
    >
      <slot />
    </div>

    <slot name="footer">
      <div class="dialog-footer dop-footer">
        <button type="button" class="btn-ghost-sm" :disabled="loading" @click="onCancel">
          {{ cancelText }}
        </button>
        <button type="button" class="btn-primary-sm" :disabled="loading" @click="onConfirm">
          {{ loading ? '保存中...' : confirmText }}
        </button>
      </div>
    </slot>
  </a-modal>
</template>

<style>
/* 跟组件同文件加载，避免依赖外链 CSS 是否命中 teleport 节点 */
.ant-modal-wrap.form-dialog.data-operation-view .ant-modal {
  max-width: calc(100vw - 48px);
}

.ant-modal-wrap.form-dialog.data-operation-view .ant-modal-content {
  border-radius: var(--radius-panel, 18px);
  overflow: hidden;
  box-shadow:
    0 24px 64px rgba(15, 40, 60, 0.14),
    0 2px 8px rgba(15, 40, 60, 0.06);
}

.ant-modal-wrap.form-dialog.data-operation-view .ant-modal-header {
  padding: 22px 28px 0;
  margin-bottom: 0;
  border-bottom: none;
}

.ant-modal-wrap.form-dialog.data-operation-view .ant-modal-title {
  font-size: 1.0625rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.ant-modal-wrap.form-dialog.data-operation-view .ant-modal-body {
  padding: 14px 28px 4px;
}

.ant-modal-wrap.form-dialog.data-operation-view .ant-modal-close {
  top: 18px;
  inset-inline-end: 18px;
}

.dop-body--2 .dialog-grid {
  display: grid !important;
  grid-template-columns: 1fr 1fr !important;
  column-gap: 20px !important;
  row-gap: 0 !important;
  align-items: start;
}

.dop-body--2 .dialog-item--full {
  grid-column: 1 / -1 !important;
}

.dop-body--2 .dialog-form .ant-form-item {
  margin-bottom: 12px;
}

.dop-footer {
  margin-top: 4px;
  padding: 16px 0 18px;
  border-top: 1px solid var(--color-border-light, rgba(15, 40, 60, 0.08));
}

@media (max-width: 720px) {
  .dop-body--2 .dialog-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
