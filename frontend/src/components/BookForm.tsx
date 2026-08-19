import { useState } from 'react'
import type { FormEvent } from 'react'
import { adminApi } from '../api/services'
import { ApiError } from '../api/client'
import type { Book } from '../types/domain'
import { Modal } from './Modal'

interface BookFormProps {
  /** Absent when adding; the update endpoint replaces the record it is given. */
  book?: Book
  onClose: () => void
  onSaved: () => void
}

export function BookForm({ book, onClose, onSaved }: BookFormProps) {
  const editing = Boolean(book)
  const [title, setTitle] = useState(book?.title ?? '')
  const [isbn, setIsbn] = useState(book?.isbn ?? '')
  const [year, setYear] = useState(String(book?.publicationYear ?? new Date().getFullYear()))
  const [authorName, setAuthorName] = useState(book?.authors?.[0]?.name ?? '')
  const [authorBio, setAuthorBio] = useState(book?.authors?.[0]?.bio ?? '')
  const [available, setAvailable] = useState(book?.available ?? true)
  const [description, setDescription] = useState(book?.description ?? '')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [looking, setLooking] = useState(false)

  /** Fills the form from the external catalogue so the librarian only confirms it. */
  async function lookup() {
    setLooking(true)
    setError(null)
    setNotice(null)
    try {
      const found = await adminApi.lookupBook(isbn)
      setTitle(found.title ?? '')
      if (found.publicationYear) setYear(String(found.publicationYear))
      if (found.description) setDescription(found.description)
      if (found.authors?.length) {
        setAuthorName(found.authors[0].name ?? '')
        setAuthorBio(found.authors[0].bio ?? '')
      }
      setNotice(`Found "${found.title}". Check the details before saving.`)
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 404
          ? 'No book found for that ISBN. Type the details in instead.'
          : err instanceof Error
            ? err.message
            : 'Lookup failed',
      )
    } finally {
      setLooking(false)
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (book) {
        // Authors are not editable here: the update only replaces the book's own fields.
        await adminApi.updateBook(book.bookId, {
          ...book,
          title,
          isbn,
          publicationYear: Number(year),
          available,
          description,
        })
      } else {
        await adminApi.createBook({
          title,
          isbn,
          publicationYear: Number(year),
          description,
          authors: [{ name: authorName, bio: authorBio }],
        })
      }
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save this book')
      setSaving(false)
    }
  }

  return (
    <Modal
      title={editing ? 'Edit book' : 'Add a book'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" type="submit" form="book-form" disabled={saving}>
            {saving ? 'Saving…' : editing ? 'Save changes' : 'Add book'}
          </button>
        </>
      }
    >
      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}
      {notice && <p className="alert ok">{notice}</p>}

      <form id="book-form" onSubmit={submit}>
        <div className="field">
          <label htmlFor="isbn">ISBN</label>
          <div className="input-row">
            <input id="isbn" value={isbn} onChange={(e) => setIsbn(e.target.value)} required />
            <button
              className="btn"
              type="button"
              onClick={lookup}
              disabled={looking || !isbn.trim()}
            >
              {looking ? 'Looking…' : 'Look up'}
            </button>
          </div>
          <span className="help">Fills the rest of the form from the Open Library catalogue.</span>
        </div>

        <div className="field">
          <label htmlFor="title">Title</label>
          <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />
        </div>

        <div className="field">
          <label htmlFor="year">Publication year</label>
          <input
            id="year"
            type="number"
            min={1000}
            max={9999}
            value={year}
            onChange={(e) => setYear(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            rows={5}
            maxLength={4000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What the book is about. Shown on its detail panel."
          />
        </div>

        {editing ? (
          <div className="field">
            <label htmlFor="available">Availability</label>
            <select
              id="available"
              value={available ? 'yes' : 'no'}
              onChange={(e) => setAvailable(e.target.value === 'yes')}
            >
              <option value="yes">Available</option>
              <option value="no">On loan</option>
            </select>
          </div>
        ) : (
          <>
            <div className="field">
              <label htmlFor="authorName">Author</label>
              <input
                id="authorName"
                value={authorName}
                onChange={(e) => setAuthorName(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="authorBio">Author biography</label>
              <textarea
                id="authorBio"
                rows={3}
                maxLength={500}
                value={authorBio}
                onChange={(e) => setAuthorBio(e.target.value)}
                placeholder="Shown on the book's detail panel."
              />
            </div>
          </>
        )}
      </form>
    </Modal>
  )
}
