<script setup lang="ts">
import { computed } from 'vue'

/** 管理页新增/编辑弹窗外壳：统一宽度、双列栅格、确认/取消 */
const props = withDefaults(
  defineProps<{
    /** v-model 控制显隐 */
    modelValue?: boolean
    title?: string
    /** 显式宽度；不传时 1 列 520 / 2 列 960 */
    width?: string | number | null
    /** 1 单列 · 2 一行两字段（会给内容区加上 dialog-grid 双列能力） */
    columns?: number
    /** 提交中：禁用按钮、确认文案变「保存中...」 */
    loading?: boolean
    confirmText?: string
    cancelText?: string
    closeOnClickModal?: boolean
    destroyOnClose?: boolean
    centered?: boolean
  }>(),
  {
    modelValue: false,
    title: '',
    width: undefined,
    columns: 1,
    loading: false,
    confirmText: '保存',
    cancelText: '取消',
    closeOnClickModal: false,
    destroyOnClose: false,
    centered: true,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const isTwoCol = computed(() => Number(props.columns) === 2)

const resolvedWidth = computed<string | number>(() => {
  if (props.width !== undefined && props.width !== null && props.width !== '') {
    return typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  return isTwoCol.value ? 960 : 520
})

const dialogClass = computed(() =>
  ['form-dialog', 'data-operation-view', isTwoCol.value ? 'is-cols-2' : 'is-cols-1']
    .join(' '),
)

function close(): void {
  emit('update:modelValue', false)
}

function onCancel(): void {
  close()
  emit('cancel')
}

/** 仅 emit confirm，实际提交逻辑由父组件 @confirm 处理 */
function onConfirm(): void {
  emit('confirm')
}

function onOpenChange(open: boolean): void {
  emit('update:modelValue', open)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="resolvedWidth"
    :class="dialogClass"
    :align-center="centered"
    :close-on-click-modal="closeOnClickModal"
    :destroy-on-close="destroyOnClose"
    @update:model-value="onOpenChange"
  >
    <div
      class="dop-body"
      :class="isTwoCol ? 'dop-body--2' : 'dop-body--1'"
      :data-cols="isTwoCol ? 2 : 1"
    >
      <slot />
    </div>

    <template #footer>
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
    </template>
  </el-dialog>
</template>

<style>
/* 跟组件同文件加载，避免依赖外链 CSS 是否命中 teleport 节点 */
.el-dialog.form-dialog.data-operation-view {
  max-width: calc(100vw - 48px);
  border-radius: var(--radius-panel, 18px);
  overflow: hidden;
  box-shadow:
    0 24px 64px rgba(15, 40, 60, 0.14),
    0 2px 8px rgba(15, 40, 60, 0.06);
}

.el-dialog.form-dialog.data-operation-view .el-dialog__header {
  padding: 22px 28px 0;
  margin-right: 0;
  border-bottom: none;
}

.el-dialog.form-dialog.data-operation-view .el-dialog__title {
  font-size: 1.0625rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.el-dialog.form-dialog.data-operation-view .el-dialog__body {
  padding: 14px 28px 4px;
}

.el-dialog.form-dialog.data-operation-view .el-dialog__headerbtn {
  top: 18px;
  right: 18px;
}

.el-dialog.form-dialog.data-operation-view .el-dialog__footer {
  padding: 0 28px;
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

.dop-body--2 .dialog-form .el-form-item {
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
