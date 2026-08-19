import { Link, Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'

/** Redirects to /login, remembering where the user was headed. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}

/**
 * Guards the screens only an administrator may open. The navigation already hides
 * them, so this only fires when someone types the URL - which deserves an
 * explanation rather than a silent redirect.
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()

  if (!isAdmin) {
    return (
      <div className="notice">
        <h2>Administrators only</h2>
        <p>Library members are not shown to other members. Ask an administrator for access.</p>
        <Link className="btn" to="/books">
          Back to books
        </Link>
      </div>
    )
  }

  return <>{children}</>
}
