import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend has no CORS configuration, so calling http://localhost:9092 directly
// from the dev server would be blocked by the browser. Instead every /api-prefixed
// path is proxied, which makes requests same-origin from the browser's point of view.
// See src/api/client.ts, which prefixes all calls with /backend.
// noinspection JSUnusedGlobalSymbols -- Loaded by name by Vite, never imported, so an IDE sees an export nobody uses.
export default defineConfig({
  // '/' locally, '/<repo>/' on GitHub Pages, where the site is served from a subdirectory and
  // absolute asset paths would otherwise 404.
  base: process.env.VITE_BASE_PATH ?? '/',
  plugins: [react()],
  server: {
    // 5174, not Vite's default 5173. Every other Vite project on the machine also defaults to
    // 5173, and whichever starts first takes it; the loser silently moves to the next free port,
    // which is how you end up with this console's URL serving somebody else's application.
    //
    // strictPort turns that into a refusal to start rather than a quiet renumbering: if 5174 is
    // taken, the only thing that can be holding it is another copy of this dev server.
    port: 5174,
    strictPort: true,
    proxy: {
      '/backend': {
        target: 'http://localhost:9092',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/backend/, ''),
      },
    },
  },
})
