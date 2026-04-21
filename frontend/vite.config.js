import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // 경로가 /api로 시작하는 요청을 대상으로 함
      '/api': {
        target: 'http://localhost:8080', // 백엔드 서버 주소 (실제 주소에 맞게 수정)
        changeOrigin: true,             // 대상 서버의 호스트 헤더를 target 주소로 변경
        rewrite: (path) => path.replace(/^\/api/, ''), // 요청 경로에서 /api를 제거하고 전달
        secure: false,                  // SSL 인증서 검증 무시 (자체 서명된 인증서 사용 시 필요)
      }
    }
  }
})
