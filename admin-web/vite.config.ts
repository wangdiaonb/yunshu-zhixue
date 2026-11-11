import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 生产由 Nginx 以 /admin 路径托管，构建 base 必须同步（D20 同域分区）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: mode === 'production' ? '/admin/' : '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5174,
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
