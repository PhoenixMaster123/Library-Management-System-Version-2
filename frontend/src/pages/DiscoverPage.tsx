import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { booksApi } from '../api/services'
import { Pagination } from '../components/Pagination'
import { EmptyState } from '../components/TableStates'
import { useApiCall } from '../hooks/useApiCall'
import type { CatalogCandidate, DiscoverPage as DiscoverResults } from '../types/domain'

const PAGE_SIZE = 20

/**
 * The rest of the world's books, not the library's. Anything found here can be put on the shelves
 * by whoever wants to read it - the catalogue belongs to the members, not to the desk.
 */
export function DiscoverPage() {
  const [term, setTerm] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [addingIsbn, setAddingIsbn] = useState<string | null>(null)
  const [added, setAdded] = useState<Set<string>>(new Set())
  const [notice, setNotice] = useState<string | null>(null)
  const [addError, setAddError] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<DiscoverResults>()

  useEffect(() => {
    if (!query) return
    run(() => booksApi.discover(query, page, PAGE_SIZE))
  }, [query, page, run])

  function search(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setNotice(null)
    setAddError(null)
    setQuery(term.trim())
  }

  async function add(book: CatalogCandidate) {
    setAddingIsbn(book.isbn)
    setAddError(null)
    setNotice(null)
    try {
      await booksApi.addFromCatalog(book)
      // Marked locally rather than re-fetching the page: the row only needs to stop offering Add.
      setAdded((current) => new Set(current).add(book.isbn))
      setNotice(`"${book.title}" is now on the shelves.`)
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Could not add this book')
    } finally {
      setAddingIsbn(null)
    }
  }

  const results = data?.data ?? []
  const searched = Boolean(query)

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>Discover</h2>
          <p className="sub">
            Search the wider catalogue and add anything the library does not hold yet.
          </p>
        </div>
      </header>

      <form className="discover-search" onSubmit={search}>
        <input
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Author, title or subject — e.g. ursula le guin"
          aria-label="Search the wider catalogue"
        />
        <button className="btn btn-primary" type="submit" disabled={loading || !term.trim()}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>

      {(error || addError) && (
        <p className="alert error" role="alert">
          {error ?? addError}
        </p>
      )}
      {notice && <p className="alert ok">{notice}</p>}

      {!searched && (
        <EmptyState title="Looking for something in particular?">
          Search by author, title or subject. Anything you find can be added to the library and
          borrowed straight away.
        </EmptyState>
      )}

      {searched && loading && !data && <p className="muted">Searching the catalogue…</p>}

      {searched && !loading && results.length === 0 && (
        <EmptyState title="Nothing matched that search">
          Try an author, or a shorter title.
        </EmptyState>
      )}

      {results.length > 0 && (
        <ul className="discover-grid">
          {results.map((book) => {
            const stocked = book.stocked || added.has(book.isbn)
            return (
              <li key={book.isbn} className="discover-card">
                {book.coverId ? (
                  <img
                    className="discover-cover"
                    src={`https://covers.openlibrary.org/b/id/${book.coverId}-M.jpg`}
                    alt=""
                    loading="lazy"
                    decoding="async"
                    onError={(event) => {
                      event.currentTarget.style.visibility = 'hidden'
                    }}
                  />
                ) : (
                  <span className="discover-cover cover-empty" aria-hidden="true" />
                )}

                <div className="discover-body">
                  <strong className="discover-title">{book.title}</strong>
                  <span className="muted">{book.authors?.join(', ') || 'Unknown author'}</span>
                  <span className="muted small">
                    {book.publicationYear > 0 ? book.publicationYear : 'Year unknown'}
                  </span>

                  {stocked ? (
                    <span className="badge ok">Already on the shelves</span>
                  ) : (
                    <button
                      className="btn btn-primary"
                      type="button"
                      onClick={() => add(book)}
                      disabled={addingIsbn === book.isbn}
                    >
                      {addingIsbn === book.isbn ? 'Adding…' : 'Add to library'}
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {results.length > 0 && (
        <Pagination
          page={page}
          totalPages={data?.totalPages ?? 0}
          totalItems={data?.totalItems ?? 0}
          pageSize={PAGE_SIZE}
          onPage={(next) => {
            setPage(next)
            window.scrollTo({ top: 0, behavior: 'smooth' })
          }}
          unit="titles"
        />
      )}
    </section>
  )
}
