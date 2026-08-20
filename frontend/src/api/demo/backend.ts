import type {
  Book, BookDetailResponse, CatalogCandidate, Customer, Page, Profile, Transaction,
} from '../../types/domain'
import { LOAN_LIMIT, db, helpers, save } from './store'

/**
 * An in-browser stand-in for the Spring backend, used when the site is published with nowhere to
 * call. It answers the same paths with the same shapes, and keeps the rules that matter: an
 * administrator sees the admin screens, a member only their own loans, a book is out or it is not,
 * and nobody borrows against somebody else's membership.
 *
 * It is not the real backend. Data is per-browser, and there is no cryptography behind the token.
 */

export class DemoHttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message)
  }
}

interface Ctx {
  username: string
  role: 'ADMIN' | 'USER'
  customerId?: string
}

const TOKEN_PREFIX = 'Bearer demo.'

function contextFrom(auth: string | null): Ctx | null {
  if (!auth?.startsWith(TOKEN_PREFIX)) return null
  const username = decodeURIComponent(auth.slice(TOKEN_PREFIX.length))
  const user = db().users.find((u) => u.username === username)
  return user ? { username: user.username, role: user.role, customerId: user.customerId } : null
}

function requireCtx(auth: string | null): Ctx {
  const ctx = contextFrom(auth)
  if (!ctx) throw new DemoHttpError(401, 'Session expired. Please sign in again.')
  return ctx
}

function requireAdmin(auth: string | null): Ctx {
  const ctx = requireCtx(auth)
  if (ctx.role !== 'ADMIN') throw new DemoHttpError(403, 'You do not have access to this area.')
  return ctx
}

function page<T>(rows: T[], p: number, size: number): Page<T> {
  const start = p * size
  return {
    data: rows.slice(start, start + size),
    totalItems: rows.length,
    currentPage: p,
    totalPages: Math.max(1, Math.ceil(rows.length / size)),
  }
}

const num = (q: URLSearchParams, k: string, d: number) => Number(q.get(k) ?? d) || d

const withAvailability = (book: Book): Book => ({
  ...book,
  available: !helpers.openLoanForBook(book.bookId),
})

const hydrate = (t: Transaction): Transaction => ({
  ...t,
  customer: helpers.customer(t.customerId),
  book: helpers.book(t.bookId),
})

/**
 * Stands in for the Open Library search. These are real books with real ISBNs, so the cover lookup
 * resolves against Open Library exactly as it does with the backend behind it - an invented ISBN
 * would leave every card blank.
 */
const CATALOGUE: Omit<CatalogCandidate, 'stocked'>[] = [
  { title: 'Nineteen Eighty-Four', isbn: '9780451524935', publicationYear: 1949, authors: ['George Orwell'], coverId: null },
  { title: 'Brave New World', isbn: '9780060850524', publicationYear: 1932, authors: ['Aldous Huxley'], coverId: null },
  { title: 'Frankenstein', isbn: '9780486282114', publicationYear: 1818, authors: ['Mary Shelley'], coverId: null },
  { title: 'Pride and Prejudice', isbn: '9780141439518', publicationYear: 1813, authors: ['Jane Austen'], coverId: null },
  { title: 'Moby-Dick', isbn: '9780142437247', publicationYear: 1851, authors: ['Herman Melville'], coverId: null },
  { title: 'The Great Gatsby', isbn: '9780743273565', publicationYear: 1925, authors: ['F. Scott Fitzgerald'], coverId: null },
  { title: 'Crime and Punishment', isbn: '9780140449136', publicationYear: 1866, authors: ['Fyodor Dostoevsky'], coverId: null },
  { title: 'The Hobbit', isbn: '9780547928227', publicationYear: 1937, authors: ['J. R. R. Tolkien'], coverId: null },
  { title: 'Jane Eyre', isbn: '9780141441146', publicationYear: 1847, authors: ['Charlotte Bronte'], coverId: null },
  { title: 'The Odyssey', isbn: '9780140268867', publicationYear: -700, authors: ['Homer'], coverId: null },
  { title: 'Slaughterhouse-Five', isbn: '9780385333849', publicationYear: 1969, authors: ['Kurt Vonnegut'], coverId: null },
  { title: 'The Handmaid’s Tale', isbn: '9780385490818', publicationYear: 1985, authors: ['Margaret Atwood'], coverId: null },
]

function catalogue(query: string): CatalogCandidate[] {
  const q = query.trim().toLowerCase()
  if (!q) return []
  const held = new Set(db().books.map((b) => b.isbn.replace(/[^0-9Xx]/g, '')))
  return CATALOGUE.filter(
    (c) =>
      c.title.toLowerCase().includes(q) ||
      c.authors.some((a) => a.toLowerCase().includes(q)) ||
      c.isbn.includes(q),
  ).map((c) => ({ ...c, stocked: held.has(c.isbn) }))
}

export function handle(method: string, path: string, body: unknown, auth: string | null): unknown {
  const [rawPath, rawQuery = ''] = path.split('?')
  const q = new URLSearchParams(rawQuery)
  const seg = rawPath.split('/').filter(Boolean)
  const payload = (body ?? {}) as Record<string, unknown>
  const state = db()

  const str = (k: string, fallback = '') => String(payload[k] ?? fallback)

  // ---- authentication ------------------------------------------------------------------------
  if (method === 'POST' && rawPath === '/api/login') {
    const user = state.users.find(
      (u) => u.username === str('username') && u.password === str('password'),
    )
    if (!user) throw new DemoHttpError(401, 'Invalid credentials')
    return {
      message: 'Signed in.',
      token: `${TOKEN_PREFIX}${encodeURIComponent(user.username)}`,
      username: user.username,
      role: user.role,
      customerId: user.customerId,
    }
  }

  if (method === 'POST' && rawPath === '/api/register') {
    const username = str('username').trim()
    if (!username) throw new DemoHttpError(400, 'A username is required.')
    if (state.users.some((u) => u.username === username)) {
      throw new DemoHttpError(409, 'That username is already taken.')
    }
    const customer: Customer = {
      customerId: helpers.uuid(),
      name: str('name', username),
      email: str('email'),
      privileges: true,
    }
    state.customers.push(customer)
    // Registration only ever creates members, exactly as RegistrationController does.
    state.users.push({
      username,
      password: str('password'),
      role: 'USER',
      customerId: customer.customerId,
    })
    save()
    return { message: 'Welcome to the library.', username, customerId: customer.customerId }
  }

  if (method === 'POST' && rawPath === '/api/logout') return { message: 'Signed out.' }

  if (method === 'GET' && rawPath === '/api/me') {
    const ctx = requireCtx(auth)
    return { username: ctx.username, role: ctx.role, customerId: ctx.customerId }
  }

  if (method === 'POST' && rawPath === '/api/change-password') {
    const ctx = requireCtx(auth)
    const user = state.users.find((u) => u.username === ctx.username)!
    if (user.password !== str('currentPassword')) {
      throw new DemoHttpError(400, 'That is not your current password.')
    }
    user.password = str('newPassword')
    save()
    return { message: 'Password changed.' }
  }

  if (method === 'GET' && rawPath === '/api/profile') {
    const ctx = requireCtx(auth)
    const customer = helpers.customer(ctx.customerId)
    const profile: Profile = {
      username: ctx.username,
      role: ctx.role,
      member: Boolean(customer),
      loanLimit: LOAN_LIMIT,
      ...(customer && {
        name: customer.name,
        email: customer.email,
        activeLoans: helpers.activeLoansFor(customer.customerId).length,
      }),
    }
    return profile
  }

  if (rawPath === '/api/reminders') {
    const ctx = requireCtx(auth)
    const customer = helpers.customer(ctx.customerId)
    if (method === 'PUT') {
      const enabled = Boolean(payload.enabled)
      state.reminders[ctx.username] = enabled
      save()
      return { message: 'Saved.', enabled, email: customer?.email ?? '' }
    }
    return {
      supported: Boolean(customer),
      enabled: state.reminders[ctx.username] ?? false,
      email: customer?.email ?? '',
    }
  }

  // ---- catalogue -----------------------------------------------------------------------------
  if (method === 'GET' && rawPath === '/books/paginated') {
    requireCtx(auth)
    const query = (q.get('query') ?? '').toLowerCase()
    const sortBy = q.get('sortBy') ?? 'title'
    let rows = state.books.map(withAvailability)
    if (query) {
      rows = rows.filter(
        (b) =>
          b.title.toLowerCase().includes(query) ||
          b.isbn.includes(query) ||
          b.authors.some((a) => a.name.toLowerCase().includes(query)),
      )
    }
    rows.sort((a, b) =>
      sortBy === 'publicationYear'
        ? a.publicationYear - b.publicationYear
        : a.title.localeCompare(b.title),
    )
    return page(rows, num(q, 'page', 0), num(q, 'size', 25))
  }

  if (method === 'GET' && rawPath === '/books/discover') {
    requireCtx(auth)
    const hits = catalogue(q.get('query') ?? '')
    return { data: hits, currentPage: 0, totalItems: hits.length, totalPages: 1 }
  }

  if (method === 'POST' && rawPath === '/books/discover') {
    requireCtx(auth)
    const names = (payload.authors as string[] | undefined) ?? []
    const book: Book = {
      bookId: helpers.uuid(),
      title: str('title'),
      isbn: str('isbn'),
      publicationYear: Number(payload.publicationYear) || 0,
      createdAt: helpers.iso(new Date()),
      authors: names.map((name) => ({ authorId: helpers.uuid(), name, bio: 'Writer.' })),
      available: true,
      description: null,
    }
    state.books.push(book)
    state.authors.push(...book.authors)
    save()
    return { message: 'Added to the shelves.', bookId: book.bookId }
  }

  if (method === 'GET' && seg[0] === 'books' && seg.length === 2) {
    const ctx = requireCtx(auth)
    const book = helpers.book(seg[1])
    if (!book) throw new DemoHttpError(404, 'We could not find that book.')
    const loan = helpers.openLoanForBook(book.bookId)
    const detail: BookDetailResponse = {
      data: withAvailability(book),
      borrowedByMe: Boolean(loan && loan.customerId === ctx.customerId),
      dueDate: loan?.dueDate ?? null,
    }
    return detail
  }

  // ---- authors -------------------------------------------------------------------------------
  if (method === 'GET' && rawPath === '/authors/paginated') {
    requireCtx(auth)
    const rows = [...state.authors].sort((a, b) => a.name.localeCompare(b.name))
    return page(rows, num(q, 'page', 0), num(q, 'size', 10))
  }

  if (method === 'GET' && seg[0] === 'authors' && seg.length === 2) {
    requireCtx(auth)
    const found = state.authors.find((a) => a.authorId === seg[1])
    if (!found) throw new DemoHttpError(404, 'We could not find that author.')
    return {
      ...found,
      books: state.books.filter((b) => b.authors.some((a) => a.authorId === found.authorId)),
    }
  }

  // ---- members (administrators only) -----------------------------------------------------------
  if (method === 'GET' && (rawPath === '/customers/paginated' || rawPath === '/customers/search')) {
    requireAdmin(auth)
    const query = (q.get('query') ?? '').toLowerCase()
    const rows = state.customers
      .filter(
        (c) =>
          !query ||
          c.name.toLowerCase().includes(query) ||
          c.email.toLowerCase().includes(query),
      )
      .sort((a, b) => a.name.localeCompare(b.name))
    return page(rows, num(q, 'page', 0), num(q, 'size', 10))
  }

  if (method === 'GET' && seg[0] === 'customers' && seg.length === 2) {
    requireAdmin(auth)
    const customer = helpers.customer(seg[1])
    if (!customer) throw new DemoHttpError(404, 'We could not find that member.')
    // Wrapped in {message, data}, which is what customersApi.byId unwraps.
    return {
      message: 'Member found.',
      data: {
        ...customer,
        transactions: state.transactions
          .filter((t) => t.customerId === customer.customerId)
          .map(hydrate),
      },
    }
  }

  if (method === 'POST' && rawPath === '/customers') {
    requireAdmin(auth)
    const name = str('name').trim()
    if (!name) throw new DemoHttpError(400, 'A name is required.')
    const customer: Customer = {
      customerId: helpers.uuid(),
      name,
      email: str('email'),
      privileges: payload.privileges !== false,
    }
    state.customers.push(customer)
    save()
    return customer
  }

  if (method === 'PUT' && seg[0] === 'customers' && seg[2] === 'privileges') {
    requireAdmin(auth)
    const customer = helpers.customer(seg[1])
    if (!customer) throw new DemoHttpError(404, 'We could not find that member.')
    // The endpoint takes a bare JSON boolean, not an object.
    customer.privileges = body === true
    save()
    return 'Customer privileges updated successfully!'
  }

  if (method === 'DELETE' && seg[0] === 'customers' && seg.length === 2) {
    requireAdmin(auth)
    const customer = helpers.customer(seg[1])
    if (!customer) throw new DemoHttpError(404, 'We could not find that member.')
    if (helpers.activeLoansFor(customer.customerId).length) {
      throw new DemoHttpError(400, 'That member still has books out.')
    }
    state.customers = state.customers.filter((c) => c.customerId !== customer.customerId)
    state.users = state.users.filter((u) => u.customerId !== customer.customerId)
    state.transactions = state.transactions.filter((t) => t.customerId !== customer.customerId)
    save()
    return 'Customer successfully deleted!'
  }

  // ---- loans ---------------------------------------------------------------------------------
  if (method === 'POST' && seg[0] === 'transactions' && seg[1] === 'borrowBook') {
    const ctx = requireCtx(auth)
    const customerId = seg[2]
    const bookId = seg[3]
    // The same rule the backend enforces: your own membership, unless you are the desk.
    if (ctx.role !== 'ADMIN' && ctx.customerId !== customerId) {
      throw new DemoHttpError(403, 'You can only borrow against your own membership.')
    }
    if (!helpers.book(bookId)) throw new DemoHttpError(404, 'We could not find that book.')
    if (helpers.openLoanForBook(bookId)) {
      throw new DemoHttpError(400, 'Failed to borrow book: Book is not available for borrowing.')
    }
    if (helpers.activeLoansFor(customerId).length >= LOAN_LIMIT) {
      throw new DemoHttpError(400, `Failed to borrow book: the loan limit of ${LOAN_LIMIT} is reached.`)
    }
    const now = new Date()
    state.transactions.push({
      transactionId: helpers.uuid(),
      customerId,
      bookId,
      borrowDate: helpers.iso(now),
      dueDate: helpers.dueDateFrom(now),
      returnDate: null,
      extended: false,
    })
    save()
    return 'Book borrowed successfully.'
  }

  if (method === 'POST' && seg[0] === 'transactions' && seg[1] === 'returnBook') {
    const ctx = requireCtx(auth)
    const loan = helpers.openLoanForBook(seg[2])
    if (!loan) {
      throw new DemoHttpError(400, 'Failed to return book: This book has no open loan to return.')
    }
    if (ctx.role !== 'ADMIN' && loan.customerId !== ctx.customerId) {
      throw new DemoHttpError(403, 'You can only return books you have out.')
    }
    loan.returnDate = helpers.iso(new Date())
    save()
    return { message: 'Transaction successful.', transactionId: loan.transactionId }
  }

  if (method === 'POST' && seg[0] === 'transactions' && seg[2] === 'extend') {
    const ctx = requireCtx(auth)
    const loan = state.transactions.find((t) => t.transactionId === seg[1])
    if (!loan) throw new DemoHttpError(404, 'We could not find that loan.')
    if (ctx.role !== 'ADMIN' && loan.customerId !== ctx.customerId) {
      throw new DemoHttpError(403, 'You can only extend your own loans.')
    }
    if (loan.extended) throw new DemoHttpError(400, 'That loan has already been extended once.')
    loan.extended = true
    loan.dueDate = helpers.dueDateFrom(new Date(loan.dueDate))
    save()
    return { message: `Loan extended until ${loan.dueDate}`, dueDate: loan.dueDate }
  }

  if (method === 'GET' && rawPath === '/transactions/me') {
    const ctx = requireCtx(auth)
    if (!ctx.customerId) return page([], 0, num(q, 'size', 20))
    const rows = state.transactions
      .filter((t) => t.customerId === ctx.customerId)
      .map(hydrate)
      .reverse()
    return page(rows, num(q, 'page', 0), num(q, 'size', 20))
  }

  if (method === 'GET' && seg[0] === 'transactions' && seg[1] === 'history') {
    requireAdmin(auth)
    const rows = state.transactions
      .filter((t) => t.customerId === seg[2])
      .map(hydrate)
      .reverse()
    return page(rows, num(q, 'page', 0), num(q, 'size', 10))
  }

  // ---- the desk ------------------------------------------------------------------------------
  if (method === 'GET' && rawPath === '/admin/loans') {
    requireAdmin(auth)
    const activeOnly = q.get('activeOnly') !== 'false'
    const rows = state.transactions
      .filter((t) => (activeOnly ? !t.returnDate : true))
      .map(hydrate)
      .reverse()
    return page(rows, num(q, 'page', 0), num(q, 'size', 20))
  }

  if (method === 'POST' && rawPath === '/admin/books') {
    requireAdmin(auth)
    const title = str('title')
    if (state.books.some((b) => b.title.toLowerCase() === title.toLowerCase())) {
      throw new DemoHttpError(400, 'Book with the same title already exists.')
    }
    const incoming = (payload.authors as { name: string; bio?: string }[] | undefined) ?? []
    const book: Book = {
      bookId: helpers.uuid(),
      title,
      isbn: str('isbn'),
      publicationYear: Number(payload.publicationYear) || 0,
      createdAt: helpers.iso(new Date()),
      authors: incoming.map((a) => ({
        authorId: helpers.uuid(),
        name: a.name,
        bio: a.bio ?? 'Writer.',
      })),
      available: true,
      description: (payload.description as string | null) ?? null,
    }
    state.books.push(book)
    state.authors.push(...book.authors)
    save()
    return book
  }

  if (method === 'PUT' && seg[0] === 'admin' && seg[1] === 'books') {
    requireAdmin(auth)
    const book = helpers.book(seg[2])
    if (!book) throw new DemoHttpError(404, 'We could not find that book.')
    book.title = str('title', book.title)
    book.isbn = str('isbn', book.isbn)
    book.publicationYear = Number(payload.publicationYear) || book.publicationYear
    book.description = (payload.description as string | null) ?? book.description
    save()
    return { message: 'Book updated.' }
  }

  if (method === 'DELETE' && seg[0] === 'admin' && seg[1] === 'books') {
    requireAdmin(auth)
    if (helpers.openLoanForBook(seg[2])) {
      throw new DemoHttpError(400, 'That book is out on loan and cannot be removed.')
    }
    state.books = state.books.filter((b) => b.bookId !== seg[2])
    save()
    return { message: 'Book removed.' }
  }

  if (method === 'GET' && rawPath === '/admin/books/lookup') {
    requireAdmin(auth)
    // There is no external catalogue here; the form is filled in by hand instead.
    throw new DemoHttpError(404, 'No catalogue lookup in the demo. Type the details in instead.')
  }

  if (method === 'POST' && rawPath === '/admin/books/import') {
    requireAdmin(auth)
    return { message: 'Bulk import is disabled in the demo.', imported: 0, skipped: 0 }
  }

  if (method === 'GET' && rawPath === '/admin/analytics') {
    requireAdmin(auth)
    const borrows = state.transactions.length
    const returns = state.transactions.filter((t) => t.returnDate).length

    const perBook = new Map<string, { title: string; isbn: string; borrowed: number; returned: number }>()
    for (const t of state.transactions) {
      const book = helpers.book(t.bookId)
      if (!book) continue
      const row = perBook.get(t.bookId) ?? { title: book.title, isbn: book.isbn, borrowed: 0, returned: 0 }
      row.borrowed += 1
      if (t.returnDate) row.returned += 1
      perBook.set(t.bookId, row)
    }

    return {
      summary: {
        booksTracked: perBook.size,
        totalBorrows: borrows,
        totalReturns: returns,
        currentlyOut: borrows - returns,
        // Nothing sits between the borrow and this figure here, so it is always current.
        streamConnected: true,
        lastEventAt: borrows ? new Date().toISOString() : null,
      },
      popularBooks: [...perBook.entries()]
        .map(([bookId, r]) => ({
          bookId,
          title: r.title,
          isbn: r.isbn,
          timesBorrowed: r.borrowed,
          timesReturned: r.returned,
          currentlyOut: r.borrowed - r.returned,
        }))
        .sort((a, b) => b.timesBorrowed - a.timesBorrowed)
        .slice(0, num(q, 'limit', 10)),
    }
  }

  throw new DemoHttpError(404, 'We could not find what you were looking for.')
}
