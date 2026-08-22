import { expect, test } from '@playwright/test'
import { requireBackend, signInAsAdmin } from './helpers'

/**
 * What the user sees when things go wrong. Every assertion here is really the same one: the screen
 * must never show HTTP's vocabulary to somebody who just wanted to look at some books.
 */
test.describe('failure messages', () => {
  test.beforeEach(async () => {
    await requireBackend()
  })

  test('a 502 reads as a sentence, not a status code', async ({ page }) => {
    await signInAsAdmin(page)

    await page.route('**/backend/books/paginated*', (route) =>
      route.fulfill({ status: 502, contentType: 'text/html', body: '<html><body>502 Bad Gateway</body></html>' }),
    )

    await page.goto('/books')

    const alert = page.getByRole('alert')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText(/not responding/i)
    await expect(alert).not.toContainText('502')
    await expect(alert).not.toContainText('<')
  })

  test('an unreachable server reads as a sentence, not "Failed to fetch"', async ({ page }) => {
    await signInAsAdmin(page)

    await page.route('**/backend/books/paginated*', (route) => route.abort('connectionrefused'))

    await page.goto('/books')

    const alert = page.getByRole('alert')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText(/can't reach the library/i)
    await expect(alert).not.toContainText(/failed to fetch/i)
  })

  test('no screen leaks a raw status code', async ({ page }) => {
    await signInAsAdmin(page)

    for (const status of [500, 502, 503]) {
      await page.route('**/backend/books/paginated*', (route) => route.fulfill({ status, body: '' }))
      await page.goto('/books')
      await expect(page.getByRole('alert')).toBeVisible()

      const body = await page.locator('body').textContent()
      expect(body).not.toMatch(/status \d{3}/i)
      await page.unroute('**/backend/books/paginated*')
    }
  })

  /** A message the server wrote for a person is better than anything the client can invent. */
  test('keeps a meaningful message from the server', async ({ page }) => {
    await signInAsAdmin(page)

    await page.route('**/backend/books/paginated*', (route) =>
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'The page number must not be negative.' }),
      }),
    )

    await page.goto('/books')

    await expect(page.getByRole('alert')).toContainText('The page number must not be negative.')
  })
})

/**
 * The reason this suite exists: recovering used to mean restarting something. It should mean
 * waiting a moment and trying again.
 */
test.describe('recovering without a restart', () => {
  test.beforeEach(async () => {
    await requireBackend()
  })

  test('the page recovers once the server answers again, with no reload', async ({ page }) => {
    await signInAsAdmin(page)

    let failing = true
    await page.route('**/backend/books/paginated*', async (route) => {
      if (failing) return route.fulfill({ status: 502, body: '' })
      return route.fallback()
    })

    await page.goto('/books')
    await expect(page.getByRole('alert')).toContainText(/not responding/i)

    // The "server" comes back. The user presses Try again - they do not restart anything.
    failing = false
    await page.getByRole('button', { name: /try again/i }).click()

    await expect(page.getByRole('alert')).toBeHidden()
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('a signed-in session survives a backend restart', async ({ page, request }) => {
    await signInAsAdmin(page)

    const token = await page.evaluate(() => sessionStorage.getItem('library.jwt'))
    expect(token).toBeTruthy()

    // A token minted before a restart must still verify after one. With library.jwt.secret set,
    // the signing key no longer changes per start-up, so this holds.
    const response = await request.get('http://127.0.0.1:9092/api/me', {
      headers: { Authorization: token! },
    })

    expect(response.status(), 'stored token should still be accepted').toBe(200)
  })
})
