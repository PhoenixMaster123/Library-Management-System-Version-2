import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAdmin, RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { AccountPage } from './pages/AccountPage'
import { BooksPage } from './pages/BooksPage'
import { CustomersPage } from './pages/CustomersPage'
import { DiscoverPage } from './pages/DiscoverPage'
import { InsightsPage } from './pages/InsightsPage'
import { LoansPage } from './pages/LoansPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'

export function App() {
  return (
    <AuthProvider>
      {/*
        The site is served from a subdirectory on GitHub Pages, so the router has to be told where
        it starts. Without this every path arrives as /<repo>/books, matches no route, falls to the
        catch-all and navigates to /books at the domain root - which really is a 404. BASE_URL is
        '/' during development, so this is a no-op there.
      */}
      <BrowserRouter basename={import.meta.env.BASE_URL}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            element={
              <RequireAuth>
                <Layout />
              </RequireAuth>
            }
          >
            <Route path="/books" element={<BooksPage />} />
            <Route path="/discover" element={<DiscoverPage />} />
            <Route path="/account" element={<AccountPage />} />
            <Route path="/account/:tab" element={<AccountPage />} />
            <Route
              path="/customers"
              element={
                <RequireAdmin>
                  <CustomersPage />
                </RequireAdmin>
              }
            />
            <Route
              path="/insights"
              element={
                <RequireAdmin>
                  <InsightsPage />
                </RequireAdmin>
              }
            />
            <Route
              path="/loans"
              element={
                <RequireAdmin>
                  <LoansPage />
                </RequireAdmin>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/books" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
