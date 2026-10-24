/** 菜单名称关键词 → Element Plus 图标组件（用于轨道 / 命令面板）
 * 同长度时按声明顺序先匹配；更具体的词放在通用词（如「管理」）前面。
 */
import type { Component } from 'vue'
import {
  Calendar,
  ChatDotRound,
  HomeFilled,
  Menu as MenuIcon,
  Setting,
  Lock,
  User,
  UserFilled,
  Microphone,
  Checked,
  Grid,
} from '@element-plus/icons-vue'

export const iconMap: Record<string, Component> = {
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
  管理: MenuIcon,
  个人: User,
  AI: ChatDotRound,
  日程: Calendar,
  会议: Microphone,
}

export const defaultIcon = Grid

export function getMenuIcon(name?: string): Component {
  const n = name || ''
  const entries = Object.entries(iconMap).sort((a, b) => b[0].length - a[0].length)
  for (const [keyword, icon] of entries) {
    if (n.includes(keyword)) return icon
  }
  return defaultIcon
}
