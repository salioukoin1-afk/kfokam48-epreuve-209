import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Dév : l'API Spring Boot tourne sur 8080 — le navigateur ne parle qu'au front.
    proxy: { '/api': 'http://localhost:8080' }
  }
})
