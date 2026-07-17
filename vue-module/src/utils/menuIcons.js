/** 菜单名称关键词 → Ant Design Vue 图标组件（用于轨道 / 命令面板）
 * 同长度时按声明顺序先匹配；更具体的词放在通用词（如「管理」）前面。
 */
import {
  HomeOutlined,
  AppstoreOutlined,
  MessageOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  LockOutlined,
  ControlOutlined,
} from '@ant-design/icons-vue'

export const iconMap = {
  首页: HomeOutlined,
  仪表盘: AppstoreOutlined,
  AI对话: MessageOutlined,
  日程: CalendarOutlined,
  事务: CheckSquareOutlined,
  任务: CheckSquareOutlined,
  对话: MessageOutlined,
  助手: MessageOutlined,
  系统: SettingOutlined,
  用户: UserOutlined,
  角色: TeamOutlined,
  权限: LockOutlined,
  管理: ControlOutlined,
  个人: UserOutlined,
}

export const defaultIcon = AppstoreOutlined

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
