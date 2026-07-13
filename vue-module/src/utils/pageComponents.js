const viewModules = import.meta.glob('../views/system/**/*.vue')

const loadersByFileName = new Map()

for (const [rawKey, loader] of Object.entries(viewModules)) {
  const normalized = rawKey.replace(/\\/g, '/')
  const fileName = normalized.split('/').pop()
  if (fileName && loader) {
    loadersByFileName.set(fileName, loader)
  }
}

/** 按 component_path 解析 glob loader */
export function getViewLoaderByComponentPath(componentPath) {
  if (!componentPath) return undefined
  const normalized = componentPath.replace(/\\/g, '/').replace(/^\//, '')
  const fileName = normalized.split('/').pop()
  if (fileName && loadersByFileName.has(fileName)) {
    return loadersByFileName.get(fileName)
  }
  const globKey = normalized.startsWith('views/')
    ? `../${normalized}`
    : `../views/${normalized}`
  return viewModules[globKey]
}

/** 按 PascalCase router_name 解析 loader，如 DashboardPage → DashboardPage.vue */
export function getViewLoaderByRouterName(routerName) {
  if (!routerName) return undefined
  const trimmed = String(routerName).trim()
  if (trimmed.startsWith('/')) return undefined
  const fileName = /\.vue$/i.test(trimmed) ? trimmed : `${trimmed}.vue`
  if (loadersByFileName.has(fileName)) {
    return loadersByFileName.get(fileName)
  }
  const base = trimmed.replace(/Page$/i, '')
  for (const [name, loader] of loadersByFileName.entries()) {
    if (name.replace(/\.vue$/i, '') === trimmed.replace(/\.vue$/i, '')) {
      return loader
    }
    if (name.replace(/\.vue$/i, '').includes(base)) {
      return loader
    }
  }
  return undefined
}

export function resolveViewLoader(menu) {
  return (
    getViewLoaderByComponentPath(menu.component) ||
    getViewLoaderByRouterName(menu.routerName) ||
    getViewLoaderByComponentPath(menu.path?.replace(/^\//, 'views/') + '.vue')
  )
}
