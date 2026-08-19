import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi, booksApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { BookDetail } from '../components/BookDetail'
import { BookForm } from '../components/BookForm'
import { CatalogImport } from '../components/CatalogImport'
import { Pagination } from '../components/Pagination'
import { EmptyState, SearchBox, SkeletonRows } from '../components/TableStates'
import { ErrorNotice } from '../components/ErrorNotice'
import { useApiCall } from '../hooks/useApiCall'
import type { Book, Page } from '../types/domain'

const DEFAULT_PAGE_SIZE = 25

export function BooksPage() {
  const { isAdmin } = useAuth()
  const [term, setTerm] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE)
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [editing, setEditing] = useState<Book | null>(null)
  const [adding, setAdding] = useState(false)
  const [importing, setImporting] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<Page<Book>>()

  const columns = isAdmin ? 6 : 5
  const refresh = useCallback(() => setReloadKey((key) => key + 1), [])

  // Debounced so typing does not fire a request per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(term.trim()), 300)
    return () => clearTimeout(timer)
  }, [term])

  // A narrower search or a bigger page makes the page you were on meaningless, so go back to the
  // first one rather than landing on an empty page 12.
  useEffect(() => {
    setPage(0)
  }, [query, size])

  useEffect(() => {
    run(() => booksApi.paginated(page, size, 'title', query))
  }, [query, page, size, reloadKey, run])

  // Deleting the last book on the last page would otherwise strand the reader on a page that no
  // longer exists, showing "the catalogue is empty" over a full catalogue.
  useEffect(() => {
    if (data && data.totalItems > 0 && page > 0 && page >= data.totalPages) {
      setPage(data.totalPages - 1)
    }
  }, [data, page])

  async function remove(book: Book) {
    if (!window.confirm(`Delete "${book.title}" from the catalogue?`)) return
    setActionError(null)
    try {
      await adminApi.deleteBook(book.bookId)
      refresh()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Could not delete this book')
    }
  }

  function goToPage(next: number) {
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const books = data?.data ?? []
  const showSkeleton = loading && !data

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>Books</h2>
          <p className="sub">Everything on the shelves, and what is currently on loan.</p>
        </div>
        <div className="head-actions">
          <Link className="btn" to="/discover">
            Discover more
          </Link>
          {isAdmin && (
            <>
              <button className="btn" type="button" onClick={() => setImporting(true)}>
                Import in bulk
              </button>
              <button className="btn btn-primary" type="button" onClick={() => setAdding(true)}>
                Add book
              </button>
            </>
          )}
        </div>
      </header>

      <SearchBox value={term} onChange={setTerm} placeholder="Search by title, ISBN or author" />

      {(error || actionError) && (
        <ErrorNotice
          message={(error ?? actionError) as string}
          onRetry={() => {
            setActionError(null)
            refresh()
          }}
        />
      )}

      <div className="table-wrap">
        <div className="table-scroll">
          <table className={`books-table${isAdmin ? ' has-actions' : ''}`}>
            <thead>
              <tr>
                <th className="col-title">Title</th>
                <th className="col-author">Author</th>
                <th className="col-isbn">ISBN</th>
                <th className="col-year">Year</th>
                <th className="col-status">Status</th>
                {isAdmin && <th className="col-actions" aria-label="Actions" />}
              </tr>
            </thead>
            <tbody>
              {showSkeleton && <SkeletonRows columns={columns} />}

              {!showSkeleton &&
                books.map((book) => (
                  <tr
                    key={book.bookId}
                    className="row-clickable"
                    tabIndex={0}
                    onClick={() => setSelectedId(book.bookId)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        setSelectedId(book.bookId)
                      }
                    }}
                  >
                    <td className="cell-title">
                      <span className="cell-clamp">{book.title}</span>
                    </td>
                    <td>
                      <span className="cell-clamp">
                        {book.authors?.map((author) => author.name).join(', ') || '—'}
                      </span>
                    </td>
                    <td className="mono">{book.isbn}</td>
                    {/* The catalogue does not always know a year, and "0" is not an answer. */}
                    <td>{book.publicationYear > 0 ? book.publicationYear : '—'}</td>
                    <td>
                      <span className={book.available ? 'badge ok' : 'badge out'}>
                        {book.available ? 'Available' : 'On loan'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td className="cell-actions" onClick={(event) => event.stopPropagation()}>
                        <button className="btn btn-ghost" type="button" onClick={() => setEditing(book)}>
                          Edit
                        </button>
                        <button className="btn btn-ghost danger" type="button" onClick={() => remove(book)}>
                          Delete
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        {!showSkeleton && books.length === 0 && (
          <EmptyState title={query ? 'No books match that search' : 'The catalogue is empty'}>
            {query
              ? 'Try a different title, ISBN or author — or look for it under Discover more.'
              : 'Add a book, or stock the shelves from Discover more.'}
          </EmptyState>
        )}
      </div>

      {!showSkeleton && (
        <Pagination
          page={page}
          totalPages={data?.totalPages ?? 0}
          totalItems={data?.totalItems ?? 0}
          pageSize={size}
          onPage={goToPage}
          onPageSize={setSize}
          unit={query ? 'results' : 'books'}
        />
      )}

      {selectedId && (
        <BookDetail
          bookId={selectedId}
          onClose={() => setSelectedId(null)}
          onChanged={refresh}
          onEdit={(book) => {
            setSelectedId(null)
            setEditing(book)
          }}
        />
      )}

      {importing && (
        <CatalogImport onClose={() => setImporting(false)} onImported={refresh} />
      )}

      {(adding || editing) && (
        <BookForm
          book={editing ?? undefined}
          onClose={() => {
            setAdding(false)
            setEditing(null)
          }}
          onSaved={() => {
            setAdding(false)
            setEditing(null)
            refresh()
          }}
        />
      )}
    </section>
  )
}
