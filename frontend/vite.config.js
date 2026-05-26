import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8083'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'iconify-icon',
        },
      },
    }),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    host:'0.0.0.0',
    proxy: {
      // 경로가 /api로 시작하는 요청을 대상으로 함
      '/api': {
        target: apiProxyTarget,          // 로컬 app 기본 포트. 게이트웨이를 쓰면 VITE_API_PROXY_TARGET=http://localhost:8080
        changeOrigin: true,             // 대상 서버의 호스트 헤더를 target 주소로 변경
        rewrite: (path) => path.replace(/^\/api/, ''), // 요청 경로에서 /api를 제거하고 전달
        secure: false,                  // SSL 인증서 검증 무시 (자체 서명된 인증서 사용 시 필요)
      },
    },
  },
})
