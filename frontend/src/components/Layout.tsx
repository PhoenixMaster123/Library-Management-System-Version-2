import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg
        width="15"
        height="15"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
      </svg>
    </span>
  )
}

/** The account menu behind the user chip: everything about you, out of the main navigation. */
function UserMenu() {
  const { session, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const container = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return

    function onPointerDown(event: MouseEvent) {
      if (!container.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function go(path: string) {
    setOpen(false)
    navigate(path)
  }

  async function handleLogout() {
    setOpen(false)
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="user-menu" ref={container}>
      <button
        className="user-chip"
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <span className="avatar" aria-hidden="true">
          {(session?.username ?? '?').slice(0, 2)}
        </span>
        <span className="user-name">
          {session?.username}
          {isAdmin && <small>Administrator</small>}
        </span>
        <span className="caret" aria-hidden="true">
          ▾
        </span>
      </button>

      {open && (
        <div className="menu" role="menu">
          <div className="menu-head">
            <strong>{session?.username}</strong>
            {isAdmin && <small>Administrator</small>}
          </div>
          {/* Staff accounts hold no membership, so there is nothing for them to borrow or show. */}
          {session?.customerId && (
            <>
              <button type="button" role="menuitem" onClick={() => go('/account')}>
                My loans
              </button>
              <button type="button" role="menuitem" onClick={() => go('/account/settings')}>
                Settings
              </button>
            </>
          )}
          {!session?.customerId && (
            <button type="button" role="menuitem" onClick={() => go('/account/settings')}>
              Settings
            </button>
          )}
          <div className="menu-sep" />
          <button type="button" role="menuitem" className="danger" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

export function Layout() {
  const { isAdmin } = useAuth()

  return (
    <div className="app">
      <nav className="nav">
        <div className="nav-inner">
          <span className="brand">
            <BrandMark />
            Library
          </span>

          <div className="nav-links">
            <NavLink to="/books">Books</NavLink>
            <NavLink to="/discover">Discover</NavLink>
            {/* The admin screens are guarded server-side too; hiding them just avoids dead ends. */}
            {isAdmin && <NavLink to="/loans">Loans</NavLink>}
            {isAdmin && <NavLink to="/customers">Customers</NavLink>}
            {isAdmin && <NavLink to="/insights">Insights</NavLink>}
          </div>

          <div className="nav-right">
            <UserMenu />
          </div>
        </div>
      </nav>

      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
