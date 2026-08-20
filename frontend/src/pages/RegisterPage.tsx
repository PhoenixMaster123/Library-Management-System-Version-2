import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { BrandMark } from '../components/Layout'
import { PasswordField } from '../components/PasswordField'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/books" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      // Creates the account, the library membership, and signs the new member in.
      await register({ name, email, username, password })
      navigate('/books', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="auth-brand">
          <BrandMark />
          Library
        </div>

        <h1>Create your account</h1>
        <p className="sub">Signing up also registers you as a library member.</p>

        {error && (
          <p className="alert error" role="alert">
            {error}
          </p>
        )}

        <div className="field">
          <label htmlFor="name">Full name</label>
          <input
            id="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoComplete="name"
            required
            autoFocus
          />
        </div>

        <div className="field">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="new-username">Username</label>
          <input
            id="new-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            minLength={3}
            maxLength={50}
            required
          />
          <span className="help">3–50 characters, used to sign in.</span>
        </div>

        <PasswordField
          id="new-password"
          label="Password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          required
          help="At least 6 characters."
        />

        <button className="btn btn-primary btn-block" type="submit" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>

        <p className="switch-link">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  )
}
