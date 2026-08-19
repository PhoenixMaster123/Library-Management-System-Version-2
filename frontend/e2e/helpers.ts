import { expect, type Page, test } from '@playwright/test'

export const BACKEND = process.env.E2E_BACKEND_URL ?? 'http://127.0.0.1:9092'

/**
 * These tests read real data, so they need the Spring backend. Skip with an explanation rather
 * than fail: a missing backend is not a broken frontend, and a red suite that means "you forgot to
 * start the server" trains people to ignore red suites.
 */
export async function requireBackend() {
  try {
    const response = await fetch(`${BACKEND}/login`)
    test.skip(!response.ok, `Backend on ${BACKEND} answered ${response.status}.`)
  } catch {
    test.skip(true, `No backend on ${BACKEND}. Start it with: ./mvnw spring-boot:run`)
  }
}

/** Signs in as the administrator DataInitializer bootstraps at start-up. */
export async function signInAsAdmin(page: Page) {
  await page.goto('/login')
  await page.locator('input#username').fill('admin')
  await page.locator('input#password').fill(process.env.LIBRARY_ADMIN_PASSWORD ?? 'admin')
  await page.locator('button[type=submit]').click()
  await expect(page).toHaveURL(/\/books$/)
}

/** Registers a throwaway member, which is the only way to get a non-admin account. */
export async function registerMember(page: Page) {
  const suffix = Date.now().toString(36)
  const username = `e2e_${suffix}`

  await page.goto('/register')
  await page.locator('input#name').fill(`E2E ${suffix}`)
  await page.locator('input#email').fill(`${username}@example.test`)
  await page.locator('input#new-username').fill(username)
  await page.locator('input#new-password').fill('e2e-password')
  await page.locator('button[type=submit]').click()
  await expect(page).toHaveURL(/\/books$/)

  return username
}
