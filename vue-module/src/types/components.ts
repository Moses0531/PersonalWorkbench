/** 通用视图组件的 props 配置类型（ListView 系列组件共享） */

/** 属性值支持静态值或 (row, context) => value 函数形式 */
export type ActionValue =
  | string
  | number
  | boolean
  | ((row: object, context: object) => string | number | boolean | null | undefined)

/** 表格行操作项（ManageRowActions 的 actions 配置） */
export interface RowAction {
  key?: string
  permission?: string
  label?: ActionValue
  title?: ActionValue
  class?: string
  visible?: ActionValue
  disabled?: ActionValue
  onClick?: (row: object, context: object) => void
}

/** 列定义（FlatManageListView / HierarchicalManageListView 的 columns 配置） */
export interface ColumnDef {
  prop?: string
  label?: string
  /** 'actions' 时渲染行操作列 */
  type?: string
  width?: string | number
  align?: string
  headerClass?: string
  cellClass?: string
  emptyText?: string
}

/** 行主键：字段名或取值函数 */
export type RowKeyConfig = string | ((row: object, index: number) => string | number)

/** 行样式/类名回调 */
export type RowClassFn = (row: object, index: number) => string | undefined | null
export type RowStyleFn = (row: object, index: number) => Record<string, string> | undefined
