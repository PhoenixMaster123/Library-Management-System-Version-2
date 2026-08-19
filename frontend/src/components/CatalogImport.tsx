import { useState } from 'react'
import type { FormEvent } from 'react'
import { adminApi } from '../api/services'
import type { CatalogCandidate } from '../types/domain'
import { Modal } from './Modal'

interface CatalogImportProps {
  onClose: () => void
  onImported: () => void
}

/** Stocks the library from Open Library: search, tick what you want, import in one go. */
export function CatalogImport({ onClose, onImported }: CatalogImportProps) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<CatalogCandidate[] | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [searching, setSearching] = useState(false)
  const [importing, setImporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [summary, setSummary] = useState<string | null>(null)

  async function search(event: FormEvent) {
    event.preventDefault()
    setSearching(true)
    setError(null)
    setSummary(null)
    try {
      const found = await adminApi.searchCatalog(query)
      setResults(found)
      setSelected(new Set(found.map((book) => book.isbn)))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
    } finally {
      setSearching(false)
    }
  }

  function toggle(isbn: string) {
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(isbn)) next.delete(isbn)
      else next.add(isbn)
      return next
    })
  }

  async function importSelected() {
    setImporting(true)
    setError(null)
    try {
      const result = await adminApi.importBooks([...selected])
      setSummary(result.message)
      onImported()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed')
    } finally {
      setImporting(false)
    }
  }

  return (
    <Modal
      title="Import from catalogue"
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>
            Close
          </button>
          <button
            className="btn btn-primary"
            type="button"
            onClick={importSelected}
            disabled={importing || selected.size === 0}
          >
            {importing ? 'Importing…' : `Import ${selected.size} book(s)`}
          </button>
        </>
      }
    >
      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}
      {summary && <p className="alert ok">{summary}</p>}

      <form onSubmit={search}>
        <div className="field">
          <label htmlFor="catalog-query">Search Open Library</label>
          <div className="input-row">
            <input
              id="catalog-query"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Author, title or subject - e.g. tolkien"
              required
            />
            <button className="btn" type="submit" disabled={searching || !query.trim()}>
              {searching ? 'Searching…' : 'Search'}
            </button>
          </div>
          <span className="help">Books already on the shelves are skipped on import.</span>
        </div>
      </form>

      {results && results.length === 0 && (
        <p className="muted">Nothing matched that search. Try an author or a shorter title.</p>
      )}

      {results && results.length > 0 && (
        <>
          <div className="candidate-bar">
            <span className="muted">
              {results.length} found · {selected.size} selected
            </span>
            <button
              className="btn btn-ghost"
              type="button"
              onClick={() =>
                setSelected(
                  selected.size === results.length
                    ? new Set()
                    : new Set(results.map((book) => book.isbn)),
                )
              }
            >
              {selected.size === results.length ? 'Clear all' : 'Select all'}
            </button>
          </div>

          <ul className="candidate-list">
            {results.map((book) => (
              <li key={book.isbn} className={selected.has(book.isbn) ? 'picked' : undefined}>
                <label>
                  <input
                    type="checkbox"
                    checked={selected.has(book.isbn)}
                    onChange={() => toggle(book.isbn)}
                  />
                  {book.coverId ? (
                    <img
                      className="cover"
                      src={`https://covers.openlibrary.org/b/id/${book.coverId}-S.jpg`}
                      alt=""
                      width={35}
                      height={50}
                      loading="lazy"
                      decoding="async"
                      onError={(event) => {
                        event.currentTarget.style.visibility = 'hidden'
                      }}
                    />
                  ) : (
                    <span className="cover cover-empty" aria-hidden="true" />
                  )}
                  <span className="candidate-text">
                    <strong>{book.title}</strong>
                    <small className="muted">
                      {book.authors?.join(', ') || 'Unknown author'}
                      {book.publicationYear ? ` · ${book.publicationYear}` : ''}
                    </small>
                    <small className="mono muted">{book.isbn}</small>
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </>
      )}
    </Modal>
  )
}
