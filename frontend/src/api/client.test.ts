import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, OfflineError, api, getToken, setToken } from './client'

function respond(status: number, body = '', contentType = 'application/json') {
  return new Response(body, { status, headers: { 'Content-Type': contentType } })
}

const fetchMock = vi.fn()

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

/**
 * The messages a person actually reads. A bare "Request failed with status 502" is a fact about
 * HTTP, not about the library, so none of these may leak a status code.
 */
describe('error messages', () => {
  it('never shows a raw status code when the server sends no message', async () => {
    fetchMock.mockResolvedValue(respond(502))

    await expect(api.get('/books')).rejects.toThrow(
      /server is not responding.*restarting/i,
    )
  })

  it.each([502, 503, 504])('explains a %i as the server being unavailable', async (status) => {
    fetchMock.mockResolvedValue(respond(status))

    await expect(api.get('/books')).rejects.toThrow(/not responding/i)
  })

  it.each([500, 502, 503, 504, 400, 404, 409, 429])(
    'produces no "status %i" wording',
    async (status) => {
      fetchMock.mockResolvedValue(respond(status))

      await expect(api.get('/books')).rejects.toThrow(
        expect.objectContaining({ message: expect.not.stringContaining(String(status)) }),
      )
    },
  )

  it('prefers the message the server actually sent', async () => {
    fetchMock.mockResolvedValue(respond(409, JSON.stringify({ message: 'That ISBN already exists.' })))

    await expect(api.get('/books')).rejects.toThrow('That ISBN already exists.')
  })

  it('uses a plain-text body when that is what the endpoint returns', async () => {
    fetchMock.mockResolvedValue(respond(400, 'A book must have at least one author.', 'text/plain'))

    await expect(api.get('/books')).rejects.toThrow('A book must have at least one author.')
  })

  /** The Vite proxy and Spring's error page both answer with HTML when the backend is down. */
  it('never shows an HTML error page to the user', async () => {
    fetchMock.mockResolvedValue(
      respond(502, '<!doctype html><html><body><h1>502 Bad Gateway</h1></body></html>', 'text/html'),
    )

    const error = (await api.get('/books').catch((e: unknown) => e)) as Error

    expect(error.message).not.toContain('<')
    expect(error.message).toMatch(/not responding/i)
  })

  it('does not dump a wall of text at the user', async () => {
    fetchMock.mockResolvedValue(respond(500, 'x'.repeat(5000), 'text/plain'))

    const error = (await api.get('/books').catch((e: unknown) => e)) as Error

    expect(error.message.length).toBeLessThan(300)
  })
})

describe('when the server cannot be reached at all', () => {
  it('explains it in words rather than "Failed to fetch"', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = (await api.get('/books').catch((e: unknown) => e)) as Error

    expect(error).toBeInstanceOf(OfflineError)
    expect(error.message).not.toMatch(/failed to fetch/i)
    expect(error.message).toMatch(/can't reach the library/i)
  })

  /** Distinguishable from a real HTTP failure, so callers can treat it differently. */
  it('reports status 0, since there was no response', async () => {
    fetchMock.mockRejectedValue(new TypeError('NetworkError'))

    const error = (await api.get('/books').catch((e: Error) => e)) as ApiError

    expect(error.status).toBe(0)
  })
})

describe('authentication', () => {
  it('drops the stored token on 401 so the next render can bounce to /login', async () => {
    setToken('Bearer stale-token')
    fetchMock.mockResolvedValue(respond(401))

    await expect(api.get('/books')).rejects.toMatchObject({ status: 401 })
    expect(getToken()).toBeNull()
  })

  /** 403 means the token is fine but the door is closed - signing the user out would be wrong. */
  it('keeps the token on 403', async () => {
    setToken('Bearer good-token')
    fetchMock.mockResolvedValue(respond(403, JSON.stringify({ message: 'Administrators only.' })))

    await expect(api.get('/customers')).rejects.toThrow('Administrators only.')
    expect(getToken()).toBe('Bearer good-token')
  })

  it('sends the token as a Bearer header, adding the prefix when missing', async () => {
    setToken('raw-token')
    fetchMock.mockResolvedValue(respond(200, '{}'))

    await api.get('/books')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer raw-token')
  })

  it('does not double up a prefix the backend already added', async () => {
    setToken('Bearer already-prefixed')
    fetchMock.mockResolvedValue(respond(200, '{}'))

    await api.get('/books')

    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer already-prefixed')
  })
})

/** Backend quirks the client exists to absorb, so no page has to know about them. */
describe('backend quirks', () => {
  it('turns the 404-instead-of-an-empty-page into an empty page', async () => {
    fetchMock.mockResolvedValue(respond(404, 'No books found'))

    await expect(api.getPage('/books/paginated?page=9')).resolves.toEqual({
      data: [],
      totalPages: 0,
      currentPage: 0,
      totalItems: 0,
    })
  })

  it('turns a 404 from a search into an empty list', async () => {
    fetchMock.mockResolvedValue(respond(404, 'Nothing matched'))

    await expect(api.getList('/books?query=zzz')).resolves.toEqual([])
  })

  it('still raises other failures from a paginated endpoint', async () => {
    fetchMock.mockResolvedValue(respond(500))

    await expect(api.getPage('/books/paginated')).rejects.toBeInstanceOf(ApiError)
  })

  it('returns plain text as-is when the body is not JSON', async () => {
    fetchMock.mockResolvedValue(respond(200, 'Book borrowed successfully', 'text/plain'))

    await expect(api.post('/transactions/borrowBook/1/2')).resolves.toBe('Book borrowed successfully')
  })

  it('handles an empty 204 body', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(api.delete('/admin/books/1')).resolves.toBeUndefined()
  })
})
