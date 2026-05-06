/// <reference types="vite/client" />
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { nodePolyfills } from 'vite-plugin-node-polyfills'

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    vueDevTools(),
    nodePolyfills({
      protocolImports: true, // 启用协议导入支持
    }),
  ],
  server: {
    proxy: {
      '/api':{
        target: import.meta.env.VITE_API_URL,
        changeOrigin: true,
        secure: false,
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  define: {
    global: 'globalThis',
  },
  build: {
    target: 'chrome80',
  },
})
