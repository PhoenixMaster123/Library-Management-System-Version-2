import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { DEMO_MODE } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { BrandMark } from '../components/Layout'
import { PasswordField } from '../components/PasswordField'

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const from = (location.state as { from?: string } | null)?.from ?? '/books'

  if (isAuthenticated) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
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

        <h1>Welcome back</h1>
        <p className="sub">Sign in with your username and password.</p>

        {DEMO_MODE && (
          <p className="alert warn" role="status">
            <strong>Demo.</strong> This copy runs entirely in your browser, so everything works but
            the data is yours alone. Sign in as <code>admin</code> / <code>admin</code> to see the
            admin screens, <code>ada</code> / <code>ada</code> as a member, or register your own.
          </p>
        )}

        {error && (
          <p className="alert error" role="alert">
            {error}
          </p>
        )}

        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
            autoFocus
          />
        </div>

        <PasswordField
          id="password"
          label="Password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
          required
        />

        <button className="btn btn-primary btn-block" type="submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="switch-link">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </div>
  )
}
