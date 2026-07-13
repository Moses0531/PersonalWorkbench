const viewModules = import.meta.glob('../views/system/**/*.vue')

const loadersByFileName = new Map()

for (const [rawKey, loader] of Object.entries(viewModules)) {
  const normalized = rawKey.replace(/\\/g, '/')
  const fileName = normalized.split('/').pop()
  if (fileName && loader) {
    loadersByFileName.set(fileName, loader)
  }
}

/** 按 router_name（组件名）解析 glob loader，如 DashboardPage → DashboardPage.vue */
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
  return getViewLoaderByRouterName(menu.routerName)
}
