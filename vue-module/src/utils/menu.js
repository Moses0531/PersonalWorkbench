/** D-M-F 权限类型，与 sys_permission.type 一一对应 */
export const MENU_TYPE = {
  DIRECTORY: 'D',
  MENU: 'M',
  FUNCTION: 'F',
}

export function isDirectoryType(item) {
  return item?.menuType === MENU_TYPE.DIRECTORY
}

export function isMenuType(item) {
  return item?.menuType === MENU_TYPE.MENU
}

export function isFunctionType(item) {
  return item?.menuType === MENU_TYPE.FUNCTION
}

function looksLikeRoutePath(value) {
  const s = String(value ?? '').trim()
  return s.startsWith('/')
}

function looksLikeComponentName(value) {
  const s = String(value ?? '').trim()
  if (!s || s.startsWith('/')) return false
  if (/\.vue$/i.test(s)) return true
  return /Page$/i.test(s) || /^[A-Z][A-Za-z0-9]*$/.test(s)
}

function pathToComponentName(path) {
  const segment = String(path ?? '').replace(/^\//, '').split('/').filter(Boolean).pop() || ''
  if (!segment) return ''
  return (
    segment
      .split('-')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join('') + 'Page'
  )
}

/** 兼容旧库：router_name 与 component_path 曾互换语义 */
function coalescePermissionFields(permission) {
  let routerName = String(permission.routerName ?? permission.router_name ?? '').trim()
  let routePath = String(permission.componentPath ?? permission.component_path ?? '').trim()

  if (looksLikeRoutePath(routerName) && looksLikeComponentName(routePath)) {
    return { routerName: routePath, routePath: routerName }
  }
  if (looksLikeRoutePath(routePath) && looksLikeComponentName(routerName)) {
    return { routerName, routePath }
  }
  if (looksLikeRoutePath(routerName) && !routePath) {
    return { routerName: pathToComponentName(routerName), routePath: routerName }
  }
  return { routerName, routePath }
}

/** 后端 SysPermission → 前端统一菜单结构（唯一适配点）
 * router_name  → Vue 组件名（DashboardPage）
 * component_path → 前端 URL 路径（/dashboard）
 */
export function normalizeMenuItem(permission) {
  const { routerName, routePath } = coalescePermissionFields(permission)
  return {
    menuId: permission.permissionId ?? permission.permission_id,
    menuName: permission.name,
    menuType: permission.type,
    parentId: permission.parentId ?? permission.parent_id ?? 0,
    path: resolveMenuPath(routePath, routerName),
    routerName: routerName.replace(/\.vue$/i, '') || null,
    icon: permission.icon || '',
    visibleFlag: (permission.isDisplay ?? permission.is_display ?? 0) === 0,
    disabledFlag: (permission.status ?? '0') === '1',
    cacheFlag: false,
    code: permission.code || '',
    order: permission.order ?? 0,
  }
}

/** component_path 存 URL；router_name 仅作组件名兜底推导路径 */
function resolveMenuPath(routePath, routerName) {
  const cp = String(routePath ?? '').trim()
  if (cp.startsWith('/')) return cp
  if (cp && !cp.includes('.vue')) return cp.startsWith('/') ? cp : `/${cp}`
  const rn = String(routerName ?? '').trim()
  if (rn.startsWith('/')) return rn
  if (rn) return componentPathFromRouterName(rn)
  return null
}

export function componentPathFromRouterName(routerName) {
  const name = String(routerName ?? '').trim().replace(/Page$/i, '').replace(/\.vue$/i, '')
  if (!name) return null
  let result = ''
  for (let i = 0; i < name.length; i++) {
    const c = name.charAt(i)
    if (c >= 'A' && c <= 'Z' && i > 0) result += '-'
    result += c.toLowerCase()
  }
  return result ? `/${result}` : null
}

/** 平铺转树：仅目录 + 菜单，排除功能权限、不可见、已停用 */
export function buildMenuTree(menuList) {
  const filtered = menuList.filter(
    (menu) =>
      !isFunctionType(menu) &&
      menu.visibleFlag &&
      !menu.disabledFlag,
  )

  const roots = filtered
    .filter((menu) => menu.parentId === 0)
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || (a.menuId ?? 0) - (b.menuId ?? 0))
  for (const root of roots) {
    buildChildren(root, filtered)
  }
  return roots
}

function buildChildren(parent, allList) {
  const children = allList
    .filter((item) => item.parentId === parent.menuId)
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || (a.menuId ?? 0) - (b.menuId ?? 0))
  if (children.length === 0) return
  parent.children = children
  for (const child of children) {
    buildChildren(child, allList)
  }
}

/** 为每个叶子节点记录祖先链，供菜单展开使用 */
export function buildMenuParentIdListMap(menuTree) {
  const map = new Map()
  recursiveBuild(menuTree, [], map)
  return map
}

function recursiveBuild(menuList, parentChain, map) {
  for (const menu of menuList) {
    let chain = parentChain
    if (menu.parentId === 0) {
      chain = []
    }

    const currentChain = chain.map((item) => ({ ...item }))

    if (menu.children?.length) {
      currentChain.push({
        name: String(menu.menuId),
        title: menu.menuName,
      })
      recursiveBuild(menu.children, currentChain, map)
    } else {
      map.set(String(menu.menuId), currentChain)
    }
  }
}

/**
 * 从后端原始权限列表构建四态：
 * - menuTree：渲染树（D + M）
 * - menuRouterList：可注册路由（有 path 的 M）
 * - menuParentIdListMap：祖先链（面包屑 / 展开）
 * - functionList：功能权限（F，按钮级 code）
 */
export function buildMenuState(rawMenuList = []) {
  const menuList = rawMenuList.map(normalizeMenuItem)
  const menuTree = buildMenuTree(menuList)
  return {
    menuList,
    menuTree,
    menuRouterList: menuList.filter((item) => item.path),
    menuParentIdListMap: buildMenuParentIdListMap(menuTree),
    functionList: menuList.filter(
      (item) => isFunctionType(item) && item.visibleFlag && !item.disabledFlag,
    ),
  }
}
