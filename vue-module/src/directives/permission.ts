import { watch } from 'vue'
import type { Directive } from 'vue'
import { hasPermission } from '@/utils/permission'
import { useUserStore } from '@/stores/userStore'

type PermissionElement = HTMLElement & { _permissionStop?: () => void }

function resolveCodes(value: unknown, modifiers: Record<string, unknown>) {
  if (!value) return { codes: [] as string[], mode: 'any' as 'any' | 'all' }
  const codes = Array.isArray(value) ? (value as string[]) : [String(value)]
  const mode = modifiers.all ? ('all' as const) : ('any' as const)
  return { codes, mode }
}

function applyPermission(
  el: PermissionElement,
  binding: { value?: unknown; modifiers?: Partial<Record<string, boolean>> },
) {
  const { value } = binding
  if (value == null || value === '') return

  const { codes, mode } = resolveCodes(value, binding.modifiers ?? {})
  if (!hasPermission(codes.length === 1 ? codes[0] : codes, mode)) {
    el.style.display = 'none'
    el.setAttribute('data-permission-hidden', '1')
  } else if (el.getAttribute('data-permission-hidden') === '1') {
    el.style.display = ''
    el.removeAttribute('data-permission-hidden')
  }
}

/**
 * v-permission="'user:add'"
 * v-permission="['user:add','user:modify']"
 * v-permission.all="['a','b']" — 需全部满足
 *
 * 无权限时隐藏节点（保留 DOM，权限变更可恢复）；权限列表变化时自动重算。
 */
export const permissionDirective: Directive<PermissionElement, string | string[]> = {
  mounted(el, binding) {
    applyPermission(el, binding)
    const userStore = useUserStore()
    el._permissionStop = watch(
      () => userStore.functionList,
      () => applyPermission(el, binding),
      { deep: true },
    )
  },
  updated(el, binding) {
    applyPermission(el, binding)
  },
  unmounted(el) {
    el._permissionStop?.()
    delete el._permissionStop
  },
}
