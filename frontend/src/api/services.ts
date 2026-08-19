import { ApiError, api, clearToken, setSession, setToken } from './client'
import type {
  Author,
  Book,
  BookDetailResponse,
  CatalogCandidate,
  CreateBookRequest,
  Customer,
  DiscoverPage,
  LoanStatistics,
  LoginResponse,
  Profile,
  RegisterRequest,
  RegisterResponse,
  Session,
  Transaction,
} from '../types/domain'

export const authApi = {
  async login(username: string, password: string): Promise<Session> {
    const result = await api.post<LoginResponse>('/api/login', { username, password })
    setToken(result.token)

    const session: Session = {
      username: result.username,
      role: result.role,
      customerId: result.customerId,
    }
    setSession(session)
    return session
  },

  /** Creates both the account and the matching library membership. */
  register: (payload: RegisterRequest) => api.post<RegisterResponse>('/api/register', payload),

  /** Confirms a stored token is still valid and reports who it belongs to. */
  me: () => api.get<Session>('/api/me'),

  /** The signed-in account's own details - name, email, membership - without admin rights. */
  profile: () => api.get<Profile>('/api/profile'),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<{ message: string }>('/api/change-password', { currentPassword, newPassword }),

  async logout(): Promise<void> {
    try {
      // The API chain's logout: ends any server-side session and answers in JSON.
      await api.post('/api/logout')
    } catch {
      // Already signed out, or the backend is down - the local token still goes.
    }
    clearToken()
  },
}

/**
 * Due-date reminders. There is no address to send: they go to the email on the membership, and
 * the backend fills it in - which is also why `mine()` reports one back.
 */
export const remindersApi = {
  mine: () => api.get<{ supported: boolean; enabled: boolean; email: string }>('/api/reminders'),

  save: (enabled: boolean) =>
    api.put<{ message: string; enabled: boolean; email: string }>('/api/reminders', { enabled }),
}

export const booksApi = {
  /**
   * One page of the shelves. Passing `query` narrows it rather than switching endpoints, so a
   * search can be paged through exactly like the full catalogue.
   */
  paginated: (page = 0, size = 25, sortBy = 'title', query = '') =>
    api.getPage<Book>(
      `/books/paginated?page=${page}&size=${size}&sortBy=${sortBy}` +
        (query ? `&query=${encodeURIComponent(query)}` : ''),
    ),

  /** Wrapped in {message, data, borrowedByMe, dueDate}, unlike the search endpoints. */
  byId: (id: string) => api.getFresh<BookDetailResponse>(`/books/${id}`),

  /** The external catalogue, for books the library does not hold yet. */
  discover: (query: string, page = 0, size = 20) =>
    api.get<DiscoverPage>(
      `/books/discover?query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    ),

  /**
   * Puts a discovered book on the shelves. Open to any member, not just the desk.
   *
   * The whole hit is sent, not just its ISBN: a search hit's ISBN belongs to one edition, so
   * looking it up again server-side can stock a different language than the one on the card.
   */
  addFromCatalog: (book: CatalogCandidate) =>
    api.post<{ message: string; bookId: string }>('/books/discover', {
      title: book.title,
      isbn: book.isbn,
      publicationYear: book.publicationYear,
      authors: book.authors ?? [],
    }),
}

/** Catalogue management. The backend answers 403 for anyone who is not an administrator. */
export const adminApi = {
  createBook: (payload: CreateBookRequest) => api.post<Book>('/admin/books', payload),

  /** Prefills the add-book form from the external catalogue; 404 means "not found, type it in". */
  lookupBook: (isbn: string) =>
    api.get<CreateBookRequest>(`/admin/books/lookup?isbn=${encodeURIComponent(isbn)}`),

  /** Candidates from the external catalogue, for stocking the library in bulk. */
  searchCatalog: (query: string, limit = 20) =>
    api.get<CatalogCandidate[]>(
      `/admin/books/search?query=${encodeURIComponent(query)}&limit=${limit}`,
    ),

  importBooks: (isbns: string[]) =>
    api.post<{ message: string; imported: string[]; skipped: string[] }>('/admin/books/import', {
      isbns,
    }),

  /** Sends the whole book back: the update replaces title, ISBN, year and availability. */
  updateBook: (id: string, book: Book) => api.put<{ message: string }>(`/admin/books/${id}`, book),

  deleteBook: (id: string) => api.delete<{ message: string }>(`/admin/books/${id}`),
}

export const authorsApi = {
  paginated: (page = 0, size = 10, sortBy = 'name') =>
    api.getPage<Author>(`/authors/paginated?page=${page}&size=${size}&sortBy=${sortBy}`),

  byId: (id: string) => api.get<Author>(`/authors/${id}`),
}

/** Administrators only - the backend answers 403 for everyone else. */
export const customersApi = {
  paginated: (page = 0, size = 10, sortBy = 'name') =>
    api.getPage<Customer>(`/customers/paginated?page=${page}&size=${size}&sortBy=${sortBy}`),

  /** Wrapped in {message, data}, like the single-book endpoint. */
  async byId(id: string): Promise<Customer> {
    const result = await api.getFresh<{ data: Customer }>(`/customers/${id}`)
    return result.data
  },

  search: (query: string, page = 0, size = 10, sortBy = 'name') =>
    api.getPage<Customer>(
      `/customers/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}&sortBy=${sortBy}`,
    ),
}

export const transactionsApi = {
  /** Returns a plain-text confirmation, not JSON. */
  borrow: (customerId: string, bookId: string) =>
    api.post<string>(`/transactions/borrowBook/${customerId}/${bookId}`),

  returnBook: (bookId: string) => api.post<Transaction>(`/transactions/returnBook/${bookId}`),

  /** One further loan period. The backend refuses a second one. */
  extend: (transactionId: string) =>
    api.post<{ message: string; dueDate: string }>(`/transactions/${transactionId}/extend`),

  /** The caller's own loans; the backend reads the membership from the token. */
  mine: (page = 0, size = 20) =>
    api.getPage<Transaction>(`/transactions/me?page=${page}&size=${size}`),

  /** Administrators only - one member's history. */
  history: (customerId: string, page = 0, size = 10) =>
    api.getPage<Transaction>(`/transactions/history/${customerId}?page=${page}&size=${size}`),

  /** Administrators only - who has what out across the whole library. */
  allLoans: (activeOnly = true, page = 0, size = 20) =>
    api.getPage<Transaction>(`/admin/loans?activeOnly=${activeOnly}&page=${page}&size=${size}`),
}

export const analyticsApi = {
  /**
   * Administrators only - borrowing statistics across the library.
   *
   * Resolves to `null` when Analytics-Service is unavailable rather than throwing, in the same
   * spirit as `getPage` translating the API's 404-instead-of-an-empty-page. The caller has to be
   * able to tell "could not be read" from "there is nothing to read": zeroed tiles would claim the
   * library has never lent a book. Every other failure still throws.
   */
  overview: (limit = 10) =>
    api
      .get<LoanStatistics>(`/admin/analytics?limit=${limit}`)
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 503) return null
        throw error
      }),
}
