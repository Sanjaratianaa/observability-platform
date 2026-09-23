import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api/logs': 'http://localhost:8081',
      '/api/chatops': 'http://localhost:8083',
      '/api': 'http://localhost:8082'
    }
  }
})
