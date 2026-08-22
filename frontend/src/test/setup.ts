import '@testing-library/jest-dom/vitest'
import { afterEach, beforeEach } from 'vitest'
import { cleanup } from '@testing-library/react'

beforeEach(() => {
  // The session lives in sessionStorage; the demo-mode catalogue still uses localStorage.
  sessionStorage.clear()
  localStorage.clear()
})

afterEach(() => {
  cleanup()
})
