import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Unit tests only. The Playwright suite in e2e/ drives a real browser against a real backend
// and is run separately with `npm run test:e2e`.
// noinspection JSUnusedGlobalSymbols -- Loaded by name by Vitest, never imported, so an IDE sees an export nobody uses.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
