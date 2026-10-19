import { defineConfig } from 'vite'
import type { ProxyOptions } from 'vite'
import react from '@vitejs/plugin-react'

const apiProxy: ProxyOptions = {
  target: 'http://localhost:8080',
  changeOrigin: true,
  bypass(request) {
    delete request.headers.origin
  },
}

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': apiProxy,
    },
  },
  preview: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': apiProxy,
    },
  },
})
