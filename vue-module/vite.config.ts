import { fileURLToPath, URL } from 'node:url'

import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

/**
 * @wangeditor/editor-for-vue 的 esm 产物对 vue 使用了 default import，
 * 而 vue.runtime.esm-bundler 没有 default 导出，rolldown 构建会直接报
 * MISSING_EXPORT；此处改写为 namespace import，产物行为不变。
 */
function fixWangEditorVueImport(): Plugin {
  return {
    name: 'fix-wangeditor-vue-import',
    enforce: 'pre',
    transform(code, id) {
      if (!id.replace(/\\/g, '/').includes('@wangeditor/editor-for-vue/dist/index.esm.js')) return null
      return code.replace(/import\s+(\w+)\s+from"vue"/g, 'import*as $1 from"vue"')
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    fixWangEditorVueImport(),
    vueDevTools(),
    AutoImport({
      dts: 'src/auto-imports.d.ts',
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      dts: 'src/components.d.ts',
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
