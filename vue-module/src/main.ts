import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

import App from './App.vue'
import router from './router'
import { permissionDirective } from '@/directives/permission'
import setupEcharts from '@/plugins/echarts'

dayjs.locale('zh-cn')

const app = createApp(App)

// 全量注册图标组件，模板中可直接使用图标名
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
setupEcharts(app)
app.directive('permission', permissionDirective)

app.mount('#app')
