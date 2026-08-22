import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { clearToken, getSession, getToken, setSession } from '../api/client'
import { authApi } from '../api/services'
import type { RegisterRequest, Session } from '../types/domain'

interface AuthState {
  session: Session | null
  isAuthenticated: boolean
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  register: (payload: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  // Seeded from sessionStorage so a refresh in the same tab keeps you signed in and renders the
  // right navigation immediately; the token is re-checked against the API below. A new tab has no
  // session storage of its own, so it starts at the login page.
  const [session, setSessionState] = useState<Session | null>(() =>
    getToken() ? getSession() : null,
  )

  useEffect(() => {
    if (!getToken()) return

    let cancelled = false
    authApi
      .me()
      .then((current) => {
        if (cancelled) return
        setSession(current)
        setSessionState(current)
      })
      .catch(() => {
        // Expired or tampered token: drop it so RequireAuth sends the user to /login.
        if (cancelled) return
        clearToken()
        setSessionState(null)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    setSessionState(await authApi.login(username, password))
  }, [])

  const register = useCallback(
    async (payload: RegisterRequest) => {
      await authApi.register(payload)
      // Sign the new member straight in - they just typed these credentials.
      await login(payload.username, payload.password)
    },
    [login],
  )

  const logout = useCallback(async () => {
    await authApi.logout()
    setSessionState(null)
  }, [])

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: session !== null,
      isAdmin: session?.role === 'ADMIN',
      login,
      register,
      logout,
    }),
    [session, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
