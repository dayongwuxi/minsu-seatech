import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'happy-dom',
    globals: true
  },
  server: {
    // 25173/后端8090：避开本机其他项目已占用的 5173/8080
    port: 25173,
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
