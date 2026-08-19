import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests against a real browser.
 *
 * These need the Spring backend on :9092 - they sign in as the bootstrap administrator and read
 * real data. The dev server is started for you; the backend is not, because starting and stopping
 * someone's database mid-suite is not this file's business. Tests skip with a clear message when
 * it is absent rather than failing as though the frontend were broken.
 */
// noinspection JSUnusedGlobalSymbols -- Loaded by name by the Playwright CLI, never imported, so an IDE sees an export nobody uses.
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5199',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        // localhost, not 127.0.0.1: Vite binds to localhost, which resolves to ::1 on Windows,
        // and probing the IPv4 address instead just times out.
        command: 'npm run dev -- --port 5199 --strictPort',
        url: 'http://localhost:5199',
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
})
