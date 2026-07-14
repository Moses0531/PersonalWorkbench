<script setup>
/** 管理页新增/编辑弹窗外壳，统一确认与取消交互 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },   // v-model 控制显隐
  title: { type: String, default: '' },
  width: { type: [String, Number], default: '520px' },
  loading: { type: Boolean, default: false },      // 提交中：禁用按钮、确认文案变「保存中...」
  confirmText: { type: String, default: '保存' },
  cancelText: { type: String, default: '取消' },
  closeOnClickModal: { type: Boolean, default: false },
  destroyOnClose: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

function close() {
  emit('update:modelValue', false)
}

function onCancel() {
  close()
  emit('cancel')  // 父组件可做重置等
}

/** 仅 emit confirm，实际提交逻辑由父组件 @confirm 处理 */
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
    :width="width"
    class="form-dialog data-operation-view"
    :mask-closable="closeOnClickModal"
    :destroy-on-close="destroyOnClose"
    :footer="null"
    @update:open="onOpenChange"
  >
    <!-- 默认插槽：表单内容 -->
    <slot />

    <!-- footer 插槽可覆盖默认双按钮 -->
    <slot name="footer">
      <div class="dialog-footer">
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
