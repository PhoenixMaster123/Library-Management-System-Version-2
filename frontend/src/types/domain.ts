/**
 * Mirrors the JSON returned by the Spring backend.
 *
 * Field names follow the Java domain models exactly (bookId, not id), so these
 * types stay honest about the wire format instead of hiding a mapping layer.
 */

export interface Author {
  authorId: string
  name: string
  bio: string
  /** Present but usually empty in list responses. */
  books?: Book[]
}

export interface Book {
  bookId: string
  title: string
  isbn: string
  publicationYear: number
  createdAt: string
  authors: Author[]
  /** Serialised from `isAvailable` on the Java side. */
  available: boolean
  /** Blurb shown on the detail panel; usually filled in by the ISBN lookup. */
  description?: string | null
}

export interface Customer {
  customerId: string
  name: string
  email: string
  privileges: boolean
  transactions?: Transaction[]
}

export interface Transaction {
  transactionId: string
  customerId: string
  bookId: string
  borrowDate: string
  dueDate: string
  /** Null while the book is still out - this is what makes a loan "active". */
  returnDate: string | null
  /** A loan may be extended once; once true, the Extend action is spent. */
  extended: boolean
  customer?: Customer
  book?: Book
}

/**
 * Shape of every paginated endpoint: /books/paginated, /customers/paginated,
 * /authors/paginated. Note the backend returns 404 (not an empty page) when a
 * page has no rows - see `getPage` in ../api/client.ts.
 */
export interface Page<T> {
  data: T[]
  totalPages: number
  currentPage: number
  totalItems: number
}

export type Role = 'ADMIN' | 'USER'

/**
 * Who is signed in. The role decides which screens the client offers; customerId is the
 * membership borrowing happens against, and is absent for staff accounts such as the seeded admin.
 */
export interface Session {
  username: string
  role: Role | string
  customerId?: string
}

/** Body of POST /admin/books, and the shape the ISBN lookup answers with. */
export interface CreateBookRequest {
  title: string
  isbn: string
  publicationYear: number
  description?: string | null
  authors: { name: string; bio: string }[]
}

/**
 * A search hit from the external catalogue. `coverId` addresses the cover image directly, which
 * loads far quicker than making the cover server resolve an ISBN first.
 */
export interface CatalogCandidate {
  title: string
  isbn: string
  publicationYear: number
  authors: string[]
  coverId: number | null
  /** Set by /books/discover: the library already holds this one, so it cannot be added again. */
  stocked?: boolean
}

/** One page of /books/discover - the external catalogue, not the shelves. */
export interface DiscoverPage {
  data: CatalogCandidate[]
  currentPage: number
  totalItems: number
  totalPages: number
}

/**
 * GET /books/{id}. `borrowedByMe` is what separates "you have this out" from "somebody else
 * does" - `available` alone cannot, which is why the panel used to offer Borrow on a book the
 * reader was already holding.
 */
export interface BookDetailResponse {
  data: Book
  borrowedByMe: boolean
  /** When the book is out, the date it is due back - whoever holds it. */
  dueDate: string | null
}

/**
 * GET /api/profile - what the signed-in account may be told about itself. Staff accounts hold no
 * membership, so `member` is false and the name, email and loan count are absent.
 */
export interface Profile {
  username: string
  role: Role | string
  member: boolean
  loanLimit: number
  name?: string
  email?: string
  activeLoans?: number
}

export interface LoginResponse extends Session {
  message: string
  /** Already includes the "Bearer " prefix. */
  token: string
}

export interface RegisterRequest {
  username: string
  password: string
  name: string
  email: string
}

export interface RegisterResponse {
  message: string
  username: string
  customerId: string
}

/**
 * Borrowing statistics from Analytics-Service, by way of the library backend.
 *
 * A projection rebuilt from the `library.loans` topic, so these totals describe what the event
 * stream has seen, not what the catalogue currently holds.
 */
export interface LoanStatistics {
  summary: LoanSummary
  popularBooks: BookStat[]
}

export interface LoanSummary {
  booksTracked: number
  totalBorrows: number
  totalReturns: number
  currentlyOut: number
  /**
   * Whether Analytics-Service is actually attached to the event stream.
   *
   * False means the figures are not small — they are absent. A reachable service with no broker
   * behind it reports zeros that would otherwise read as "this library has never lent a book".
   */
  streamConnected: boolean
  /** When the last event arrived, or null if none ever has. */
  lastEventAt: string | null
}

export interface BookStat {
  bookId: string
  title: string
  isbn: string
  timesBorrowed: number
  timesReturned: number
  currentlyOut: number
}
