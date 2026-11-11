import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发环境代理：/api → 后端，/files → MinIO Bucket（生产由 Nginx 直出）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true
        },
        '/files': {
          target: env.VITE_MINIO_TARGET || 'http://127.0.0.1:9002',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/files/, `/${env.VITE_MINIO_BUCKET || 'yszx'}`)
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 1500
    }
  }
})
