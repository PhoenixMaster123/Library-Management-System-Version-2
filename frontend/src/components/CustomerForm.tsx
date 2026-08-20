import { useState } from 'react'
import type { FormEvent } from 'react'
import { customersApi } from '../api/services'
import { Modal } from './Modal'

interface CustomerFormProps {
  onClose: () => void
  onSaved: () => void
}

/**
 * Adds a membership.
 *
 * <p>Deliberately does not ask for a password: this creates the membership a loan is recorded
 * against, not a sign-in account. Registration is what pairs the two, and the backend has no
 * endpoint for an administrator to do it on someone's behalf.
 */
export function CustomerForm({ onClose, onSaved }: CustomerFormProps) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [privileges, setPrivileges] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await customersApi.create({ name: name.trim(), email: email.trim(), privileges })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'That did not work')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title="New member"
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button className="btn btn-primary" type="submit" form="member-form" disabled={busy}>
            {busy ? 'Adding…' : 'Add member'}
          </button>
        </>
      }
    >
      <form id="member-form" onSubmit={handleSubmit}>
        {error && (
          <p className="alert error" role="alert">
            {error}
          </p>
        )}

        <div className="field">
          <label htmlFor="member-name">Full name</label>
          <input
            id="member-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
            autoFocus
          />
        </div>

        <div className="field">
          <label htmlFor="member-email">Email</label>
          <input
            id="member-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>

        <label className="checkbox">
          <input
            type="checkbox"
            checked={privileges}
            onChange={(event) => setPrivileges(event.target.checked)}
          />
          <span>Can borrow straight away</span>
        </label>

        <p className="footnote">
          This creates a membership only. To sign in, they register an account themselves and it is
          matched to this record.
        </p>

      </form>
    </Modal>
  )
}
