/** 菜单名称关键词 → Element Plus 图标组件（用于轨道 / 命令面板）
 * 同长度时按声明顺序先匹配；更具体的词放在通用词（如「管理」）前面。
 */
import {
  HomeFilled,
  Grid,
  ChatDotRound,
  Checked,
  Setting,
  User,
  UserFilled,
  Lock,
  Tools,
} from '@element-plus/icons-vue'

export const iconMap = {
  首页: HomeFilled,
  仪表盘: Grid,
  AI对话: ChatDotRound,
  事务: Checked,
  待办: Checked,
  任务: Checked,
  清单: Checked,
  对话: ChatDotRound,
  助手: ChatDotRound,
  系统: Setting,
  用户: User,
  角色: UserFilled,
  权限: Lock,
  管理: Tools,
  个人: User,
}

export const defaultIcon = Grid

/** @deprecated 使用 getMenuIcon；保留别名以免外部旧引用报错 */
export function getMenuIconSvg(name) {
  return getMenuIcon(name)
}

export function getMenuIcon(name) {
  const n = name || ''
  const entries = Object.entries(iconMap).sort((a, b) => b[0].length - a[0].length)
  for (const [keyword, icon] of entries) {
    if (n.includes(keyword)) return icon
  }
  return defaultIcon
}
