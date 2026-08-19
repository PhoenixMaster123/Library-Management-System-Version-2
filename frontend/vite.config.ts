import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend has no CORS configuration, so calling http://localhost:9092 directly
// from the dev server would be blocked by the browser. Instead every /api-prefixed
// path is proxied, which makes requests same-origin from the browser's point of view.
// See src/api/client.ts, which prefixes all calls with /backend.
// noinspection JSUnusedGlobalSymbols -- Loaded by name by Vite, never imported, so an IDE sees an export nobody uses.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/backend': {
        target: 'http://localhost:9092',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/backend/, ''),
      },
    },
  },
})
