import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // F3 : la couche API appelle /api/* en relatif -> proxy vers le backend Spring Boot
      '/api': 'http://localhost:8080',
    },
  },
})
