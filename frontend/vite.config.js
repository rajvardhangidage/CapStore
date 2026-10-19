import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, forward API + uploaded images to the Spring Boot backend
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
})
