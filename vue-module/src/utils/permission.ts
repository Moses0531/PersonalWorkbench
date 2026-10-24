import { useUserStore } from '@/stores/userStore'

/** 当前用户按钮级功能权限码（F 类型） */
export function getPermissionCodes(): string[] {
  const userStore = useUserStore()
  return (userStore.functionList || [])
    .map((item) => item.code)
    .filter((code): code is string => Boolean(code))
}

/**
 * 判断是否拥有功能权限。
 * @param code 权限码，或数组（配合 mode）
 * @param mode 数组时：任一 / 全部，默认 any
 */
export function hasPermission(
  code?: string | string[] | null,
  mode: 'any' | 'all' = 'any',
): boolean {
  if (code == null || code === '') return true
  const codes = Array.isArray(code) ? code.filter(Boolean) : [code]
  if (codes.length === 0) return true

  const perms = getPermissionCodes()
  if (codes.length === 1) return perms.includes(codes[0])
  return mode === 'all'
    ? codes.every((c) => perms.includes(c))
    : codes.some((c) => perms.includes(c))
}

export function hasAnyPermission(...codes: string[]) {
  return hasPermission(codes.flat(), 'any')
}

export function hasAllPermissions(...codes: string[]) {
  return hasPermission(codes.flat(), 'all')
}
