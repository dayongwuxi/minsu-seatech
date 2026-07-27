import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  // 生产环境挂在 seabnb.axionintell.com/admin/ 子路径下
  base: '/admin/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'node',
    globals: true
  },
  server: {
    // 25174/后端8090：避开本机其他项目已占用的端口
    port: 25174,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
})
