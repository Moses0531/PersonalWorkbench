/** 菜单/权限的前端统一结构（utils/menu.ts 的输入输出类型） */

import type { IdLike, RawSysPermission } from './entities'

/** D-M-F 权限类型，与 sys_permission.type 一一对应 */
export type MenuType = 'D' | 'M' | 'F'

/** 前端统一菜单项（normalizeMenuItem 输出） */
export interface MenuItem {
  menuId: IdLike
  menuName?: string
  menuType: MenuType | string
  parentId: IdLike
  /** 前端路由路径（如 /dashboard），无路径则不可注册路由 */
  path: string | null
  /** Vue 组件名（如 DashboardPage） */
  routerName: string | null
  /** views 下相对路径（remark 解析，如 common/DashboardPage.vue） */
  viewPath: string | null
  icon: string
  visibleFlag: boolean
  disabledFlag: boolean
  cacheFlag: boolean
  code: string
  order: number
  children?: MenuItem[]
}

/** 面包屑/菜单展开用的祖先链节点 */
export interface BreadcrumbItem {
  name: string
  title?: string
}

/** buildMenuState 的四态输出 */
export interface MenuState {
  menuList: MenuItem[]
  menuTree: MenuItem[]
  menuRouterList: MenuItem[]
  menuParentIdListMap: Map<string, BreadcrumbItem[]>
  functionList: MenuItem[]
}

/** 管理页使用的统一权限实体（normalizePermissionRecord 输出，保留原始字段） */
export interface PermissionRecord {
  permissionId: IdLike
  parentId: IdLike
  name?: string
  type: string
  status?: string
  code?: string
  icon?: string
  remark?: string
  /** Vue 组件名（如 DashboardPage） */
  routerName: string
  /** 前端路由路径（如 /dashboard） */
  componentPath: string
  isDisplay: number | string
  displayOrder: number | string
  createTime?: string
  updateTime?: string
  children?: PermissionRecord[]
  /** 兼容旧库 snake_case 残留字段 */
  [key: string]: unknown
}

/** normalize 系列函数的宽松输入（兼容 camelCase / snake_case） */
export type RawPermissionInput = RawSysPermission
