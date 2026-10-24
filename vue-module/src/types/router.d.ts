/** 路由 Meta 与 axios 扩展声明 */

import 'vue-router'
import 'axios'

declare module 'vue-router' {
  interface RouteMeta {
    /** 游客页（登录后访问则跳转首页） */
    guest?: boolean
    /** 是否需要登录，默认 true */
    requiresAuth?: boolean
    id?: string
    title?: string
    icon?: string
    hideInMenu?: boolean
    keepAlive?: boolean
    componentName?: string
  }
}

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 为 true 时响应拦截器原样返回 AxiosResponse（SSE/流式场景需要完整响应） */
    rawResponse?: boolean
  }
}

export {}
