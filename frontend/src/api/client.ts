import type { Page, Session } from '../types/domain'

/**
 * Where the API lives.
 *
 * <p>Locally this is the Vite dev proxy (see vite.config.ts), which keeps requests same-origin.
 * A static deployment has no proxy, so set VITE_API_BASE_URL to the backend's own origin at build
 * time - and add CORS on the Spring side, because the requests are then cross-origin.
 */
const BASE = import.meta.env.VITE_API_BASE_URL ?? '/backend'

/**
 * True when the build knew it was going somewhere with no backend to talk to - a static host with
 * no API origin configured. The UI says so rather than letting every sign-in fail unexplained.
 */
export const BACKEND_UNCONFIGURED = import.meta.env.VITE_BACKEND_UNCONFIGURED === 'true'

const TOKEN_KEY = 'library.jwt'
const SESSION_KEY = 'library.session'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
}

/** Cached so a page refresh renders the right navigation before /api/me answers. */
export function getSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function setSession(session: Session): void {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * The request never reached the server: it is not running, it is still starting, or the
 * connection dropped. `status` is 0 because there was no response to take one from.
 */
export class OfflineError extends ApiError {
  constructor() {
    super(0, "Can't reach the library right now. It may still be starting up — try again in a moment.")
    this.name = 'OfflineError'
  }
}

/** Thrown when the token is missing or rejected, so the UI can bounce to /login. */
export class UnauthorizedError extends ApiError {
  constructor(message = 'Session expired. Please sign in again.') {
    super(401, message)
    this.name = 'UnauthorizedError'
  }
}

/**
 * Signed in, but not allowed here - a member asking for the customer list, say.
 * Deliberately distinct from Unauthorized: the token is still good, so throwing
 * the user out to /login would be wrong.
 */
export class ForbiddenError extends ApiError {
  constructor(message = 'You do not have access to this area.') {
    super(403, message)
    this.name = 'ForbiddenError'
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken()

  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    // The login endpoint returns the token already prefixed with "Bearer ".
    headers.set('Authorization', token.startsWith('Bearer ') ? token : `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${BASE}${path}`, { ...init, headers })
  } catch {
    // fetch only rejects when the request never completed: server down, DNS, dropped connection.
    // The browser's own wording for this is "Failed to fetch", which helps nobody.
    throw new OfflineError()
  }

  if (response.status === 401) {
    clearToken()
    throw new UnauthorizedError()
  }

  if (response.status === 403) {
    throw new ForbiddenError(await readError(response))
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }

  // Several endpoints return plain text (e.g. borrowBook) rather than JSON.
  try {
    return JSON.parse(text) as T
  } catch {
    return text as unknown as T
  }
}

/**
 * What to say when the server sends no usable message of its own.
 *
 * "Request failed with status 502" is a fact about HTTP, not about the library - it tells the
 * person reading it nothing they can act on. These do.
 */
function messageForStatus(status: number): string {
  if (status === 404) return "We couldn't find what you were looking for."
  if (status === 408) return 'That took too long. Please try again.'
  if (status === 409) return 'That conflicts with something already saved.'
  if (status === 429) return 'Too many requests just now. Please wait a moment and try again.'
  if (status === 502 || status === 503 || status === 504) {
    return 'The library server is not responding. It may be restarting — try again in a moment.'
  }
  if (status >= 500) return 'Something went wrong on the library server. Please try again.'
  if (status >= 400) return 'That request could not be completed.'
  return 'Something went wrong. Please try again.'
}

/**
 * A server message is worth showing only if it reads like a sentence. Spring's error pages and
 * the Vite proxy both answer with HTML when the backend is down, which must never reach the user.
 */
function usableMessage(text: string): string | null {
  const trimmed = text.trim()
  if (!trimmed) return null
  if (trimmed.startsWith('<')) return null
  if (trimmed.length > 300) return null
  return trimmed
}

async function readError(response: Response): Promise<string> {
  let text = ''
  try {
    text = await response.text()
  } catch {
    return messageForStatus(response.status)
  }

  try {
    const parsed = JSON.parse(text)
    const fromBody = parsed.message ?? parsed.error
    if (typeof fromBody === 'string') {
      return usableMessage(fromBody) ?? messageForStatus(response.status)
    }
  } catch {
    // Not JSON. Several endpoints answer in plain text, which is usually the better message.
    const plain = usableMessage(text)
    if (plain) return plain
  }

  return messageForStatus(response.status)
}

/**
 * The paginated endpoints answer 404 with an explanatory body when the requested
 * page holds no rows, rather than returning an empty page. Callers want an empty
 * list there, not an exception, so translate it.
 */
async function getPage<T>(path: string): Promise<Page<T>> {
  try {
    return await request<Page<T>>(path)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return { data: [], totalPages: 0, currentPage: 0, totalItems: 0 }
    }
    throw error
  }
}

/** Same idea for the search endpoints, which answer 404 with plain text when nothing matches. */
async function getList<T>(path: string): Promise<T[]> {
  try {
    const result = await request<T[]>(path)
    return Array.isArray(result) ? result : []
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return []
    }
    throw error
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path),

  /**
   * Skips the browser cache. GET /books/{id} is served with `Cache-Control: max-age=30`, so a
   * plain re-read straight after borrowing replays the stale body and the panel still says
   * the book is available.
   */
  getFresh: <T>(path: string) => request<T>(path, { cache: 'no-store' }),

  getPage,
  getList,
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
