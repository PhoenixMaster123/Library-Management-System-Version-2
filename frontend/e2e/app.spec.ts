import { expect, test } from '@playwright/test'
import { registerMember, requireBackend, signInAsAdmin } from './helpers'

test.beforeEach(async () => {
  await requireBackend()
})

test.describe('signing in', () => {
  test('an administrator lands on the catalogue and sees the admin tabs', async ({ page }) => {
    await signInAsAdmin(page)

    const nav = page.locator('.nav-links')
    for (const tab of ['Books', 'Discover', 'Loans', 'Customers', 'Insights']) {
      await expect(nav.getByRole('link', { name: tab, exact: true })).toBeVisible()
    }
  })

  test('a member sees only the tabs they can use', async ({ page }) => {
    await registerMember(page)

    const nav = page.locator('.nav-links')
    await expect(nav.getByRole('link', { name: 'Books', exact: true })).toBeVisible()
    await expect(nav.getByRole('link', { name: 'Discover', exact: true })).toBeVisible()
    for (const adminTab of ['Loans', 'Customers', 'Insights']) {
      await expect(nav.getByRole('link', { name: adminTab, exact: true })).toHaveCount(0)
    }
  })

  test('a member typing an admin URL is told why, not shown a blank page', async ({ page }) => {
    await registerMember(page)

    await page.goto('/insights')

    await expect(page.locator('body')).toContainText(/administrator/i)
    await expect(page.locator('.stat-grid')).toHaveCount(0)
  })

  test('bad credentials are refused in plain language', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input#username').fill('admin')
    await page.locator('input#password').fill('definitely-not-the-password')
    await page.locator('button[type=submit]').click()

    const alert = page.getByRole('alert')
    await expect(alert).toBeVisible()
    await expect(alert).not.toContainText(/status \d{3}/i)
  })
})

test.describe('the catalogue', () => {
  test('lists books', async ({ page }) => {
    await signInAsAdmin(page)

    await expect(page.locator('table tbody tr').first()).toBeVisible()
    await expect(page.locator('th', { hasText: 'Title' })).toBeVisible()
  })

  /**
   * Regression: the status pill is `white-space: nowrap` in a `table-layout: fixed` table, so a
   * column that is too narrow does not truncate - it spills over the next one. The pill used to
   * land on top of the Edit button, which made Edit unclickable.
   */
  test('the status pill never covers the row actions', async ({ page }) => {
    await signInAsAdmin(page)
    await expect(page.locator('table tbody tr').first()).toBeVisible()

    const overlaps = await page.evaluate(() => {
      const found: string[] = []
      for (const row of document.querySelectorAll('.books-table tbody tr')) {
        const pill = row.querySelector('.badge')
        const edit = row.querySelector('.cell-actions .btn')
        if (!pill || !edit) continue
        const pillBox = pill.getBoundingClientRect()
        const editBox = edit.getBoundingClientRect()
        if (pillBox.right > editBox.left) {
          found.push(`${pill.textContent?.trim()} overlaps Edit`)
        }
      }
      return found
    })

    expect(overlaps).toEqual([])
  })

  test('Edit is actually clickable for an administrator', async ({ page }) => {
    await signInAsAdmin(page)
    await expect(page.locator('table tbody tr').first()).toBeVisible()

    await page.locator('.cell-actions').first().getByRole('button', { name: 'Edit' }).click()

    await expect(page.getByRole('dialog')).toBeVisible()
  })

  test('searching for something absent says so instead of erroring', async ({ page }) => {
    await signInAsAdmin(page)

    await page.getByPlaceholder(/search/i).fill('zzzznosuchbookzzzz')

    await expect(page.locator('body')).not.toContainText(/status \d{3}/i)
    await expect(page.getByRole('alert')).toHaveCount(0)
  })
})

test.describe('insights', () => {
  test('renders either the figures or an explanation, never a status code', async ({ page }) => {
    await signInAsAdmin(page)
    await page.getByRole('link', { name: 'Insights', exact: true }).click()

    await expect(page).toHaveURL(/\/insights$/)
    await expect(page.getByRole('heading', { name: /insights/i })).toBeVisible()

    // Analytics-Service is optional, so either outcome is correct - but both must be readable.
    const tiles = page.locator('.stat-grid')
    const explanation = page.getByText(/isn't running/i)
    await expect(tiles.or(explanation).first()).toBeVisible()

    await expect(page.locator('body')).not.toContainText(/status \d{3}/i)
  })

  test('shows an explanation, not zeros, when analytics is unavailable', async ({ page }) => {
    await signInAsAdmin(page)

    await page.route('**/backend/admin/analytics*', (route) =>
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Analytics is unavailable.' }),
      }),
    )

    await page.goto('/insights')

    await expect(page.getByText(/isn't running/i)).toBeVisible()
    await expect(page.locator('.stat-grid')).toHaveCount(0)
  })
})
