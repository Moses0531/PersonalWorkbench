import type { RouteComponent } from 'vue-router'
import { pathToComponentName } from '@/utils/menu'
import type { MenuItem } from '@/types/menu'

/**
 * 构建期：Vite glob 收集 views 下全部 .vue，建立路径索引。
 * 运行期：resolveViewLoader(menu) 仅根据后端菜单字段匹配，不出现任何具体页面名。
 */
const viewModules = import.meta.glob('../views/**/*.vue')

type ViewLoader = () => Promise<unknown>
const loaderIndex = new Map<string, ViewLoader>()
/** 同名组件存在于多个目录时，禁止仅用 basename 匹配，避免歧义 */
const basenameCount = new Map<string, number>()

for (const [rawKey, loader] of Object.entries(viewModules)) {
  const relPath = rawKey.replace(/\\/g, '/').split('/views/')[1]
  if (!relPath) continue

  const relNoExt = relPath.replace(/\.vue$/i, '')
  const basename = relPath.split('/').pop()?.replace(/\.vue$/i, '') || ''

  loaderIndex.set(relPath, loader)
  loaderIndex.set(relNoExt, loader)

  if (basename) {
    basenameCount.set(basename, (basenameCount.get(basename) || 0) + 1)
  }
}

for (const [rawKey, loader] of Object.entries(viewModules)) {
  const relPath = rawKey.replace(/\\/g, '/').split('/views/')[1]
  if (!relPath) continue
  const basename = relPath.split('/').pop()?.replace(/\.vue$/i, '') || ''
  if (basename && basenameCount.get(basename) === 1) {
    loaderIndex.set(basename, loader)
  }
}

function expandLookupKeys(raw: unknown): string[] {
  const s = String(raw ?? '').trim().replace(/\\/g, '/')
  if (!s) return []

  const keys = new Set([s, s.replace(/\.vue$/i, ''), s.replace(/^views\//, '')])
  const basename = s.split('/').pop()?.replace(/\.vue$/i, '')
  if (basename && basenameCount.get(basename) === 1) {
    keys.add(basename)
  }
  return [...keys]
}

function pickLoader(...candidates: Array<string | null | undefined>): ViewLoader | undefined {
  for (const candidate of candidates) {
    for (const key of expandLookupKeys(candidate)) {
      const loader = loaderIndex.get(key)
      if (loader) return loader
    }
  }
  return undefined
}

/**
 * 按菜单项动态解析页面懒加载函数。
 * 优先级：remark 中的 viewPath → router_name → 由 component_path 推导的组件名
 */
export function resolveViewLoader(menu?: MenuItem | null): RouteComponent | undefined {
  if (!menu) return undefined
  return pickLoader(menu.viewPath, menu.routerName, pathToComponentName(menu.path)) as RouteComponent | undefined
}

/** 开发调试：返回 glob 扫描到的 views 相对路径 */
export function listRegisteredViewPaths(): string[] {
  return [...loaderIndex.keys()].filter((key) => key.includes('/'))
}
