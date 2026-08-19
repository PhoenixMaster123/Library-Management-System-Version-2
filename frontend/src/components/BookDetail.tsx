import { useCallback, useEffect, useState } from 'react'
import { booksApi, transactionsApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import type { Book, BookDetailResponse } from '../types/domain'
import { Modal } from './Modal'

interface BookDetailProps {
  bookId: string
  onClose: () => void
  onChanged: () => void
  onEdit: (book: Book) => void
}

function formatDate(value: string | null): string {
  if (!value) return ''
  const parsed = new Date(value)
  return Number.isNaN(parsed.valueOf())
    ? value
    : parsed.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}

/**
 * The full record behind a row: every field plus each author's biography, which the table has
 * no room for. Re-fetched by id rather than reusing the row, because the search endpoint
 * returns books without their authors attached - and because only this endpoint says whether
 * the loan on the book is the reader's own.
 */
export function BookDetail({ bookId, onClose, onChanged, onEdit }: BookDetailProps) {
  const { isAdmin, session } = useAuth()
  const [detail, setDetail] = useState<BookDetailResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => setDetail(await booksApi.byId(bookId)), [bookId])

  useEffect(() => {
    let cancelled = false
    booksApi
      .byId(bookId)
      .then((result) => !cancelled && setDetail(result))
      .catch((err) => !cancelled && setError(err instanceof Error ? err.message : 'Could not load this book'))
    return () => {
      cancelled = true
    }
  }, [bookId])

  async function act(action: () => Promise<unknown>, success: string, failure: string) {
    setBusy(true)
    setError(null)
    try {
      await action()
      setNotice(success)
      await load()
      onChanged()
    } catch (err) {
      setError(err instanceof Error ? err.message : failure)
    } finally {
      setBusy(false)
    }
  }

  const book = detail?.data ?? null

  // Staff accounts have no library membership, so there is nothing to borrow against.
  const canBorrow = Boolean(session?.customerId)
  const borrowedByMe = detail?.borrowedByMe ?? false
  const onLoan = book != null && !book.available

  const footer = book && (
    <>
      {isAdmin && (
        <button className="btn" type="button" onClick={() => onEdit(book)}>
          Edit
        </button>
      )}

      {/* Three states, not two: available, out to this reader, out to somebody else. Branching on
          availability alone offered Borrow on a book the reader was already holding. */}
      {!onLoan && (
        <button
          className="btn btn-primary"
          type="button"
          onClick={() =>
            act(
              () => transactionsApi.borrow(session!.customerId!, bookId),
              'Borrowed. Enjoy the read!',
              'Could not borrow this book',
            )
          }
          disabled={busy || !canBorrow}
          title={canBorrow ? undefined : 'This account has no library membership'}
        >
          {busy ? 'Working…' : 'Borrow'}
        </button>
      )}

      {onLoan && (borrowedByMe || isAdmin) && (
        <button
          className="btn btn-primary"
          type="button"
          onClick={() =>
            act(
              () => transactionsApi.returnBook(bookId),
              'Returned. Thank you!',
              'Could not return this book',
            )
          }
          disabled={busy}
        >
          {busy ? 'Working…' : 'Return'}
        </button>
      )}

      {onLoan && !borrowedByMe && !isAdmin && (
        <span className="muted">
          {detail?.dueDate ? `Due back ${formatDate(detail.dueDate)}` : 'Currently on loan'}
        </span>
      )}
    </>
  )

  return (
    <Modal title={book?.title ?? 'Book'} onClose={onClose} footer={footer}>
      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}
      {notice && <p className="alert ok">{notice}</p>}

      {!book && !error && <p className="muted">Loading…</p>}

      {book && (
        <>
          <div className="detail-head">
            {!onLoan && <span className="badge ok">Available</span>}
            {onLoan && borrowedByMe && <span className="badge mine">You have this out</span>}
            {onLoan && !borrowedByMe && <span className="badge out">On loan</span>}
            {onLoan && detail?.dueDate && (
              <span className="muted">Due back {formatDate(detail.dueDate)}</span>
            )}
          </div>

          <dl className="detail-grid">
            <div>
              <dt>ISBN</dt>
              <dd className="mono">{book.isbn}</dd>
            </div>
            <div>
              <dt>Published</dt>
              <dd>{book.publicationYear > 0 ? book.publicationYear : '—'}</dd>
            </div>
            <div>
              <dt>Added to the library</dt>
              <dd>{book.createdAt ?? '—'}</dd>
            </div>
          </dl>

          <h4 className="detail-section">About this book</h4>
          <p className="book-blurb">
            {book.description?.trim() || 'No description has been recorded for this book yet.'}
          </p>

          <h4 className="detail-section">
            {book.authors?.length === 1 ? 'Author' : 'Authors'}
          </h4>

          {book.authors?.length ? (
            <ul className="author-list">
              {book.authors.map((author) => (
                <li key={author.authorId}>
                  <strong>{author.name}</strong>
                  <p className="muted">{author.bio?.trim() || 'No biography recorded.'}</p>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">No authors recorded for this book.</p>
          )}
        </>
      )}
    </Modal>
  )
}
