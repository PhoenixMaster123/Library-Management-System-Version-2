import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { NavLink, useParams } from 'react-router-dom'
import { authApi, remindersApi, transactionsApi } from '../api/services'
import { useAuth } from '../auth/AuthContext'
import { LoansTable } from '../components/LoansTable'
import { useApiCall } from '../hooks/useApiCall'
import type { Page, Profile, Transaction } from '../types/domain'

const MAX_ACTIVE_LOANS = 3

function LoansTab() {
  const [reloadKey, setReloadKey] = useState(0)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<Page<Transaction>>()

  const refresh = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    run(() => transactionsApi.mine())
  }, [reloadKey, run])

  async function act(loan: Transaction, action: () => Promise<unknown>, success: string) {
    setBusyId(loan.transactionId)
    setActionError(null)
    setNotice(null)
    try {
      await action()
      setNotice(success)
      refresh()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'That did not work')
    } finally {
      setBusyId(null)
    }
  }

  const loans = data?.data ?? []
  const onLoan = loans.filter((loan) => !loan.returnDate)
  const remaining = Math.max(0, MAX_ACTIVE_LOANS - onLoan.length)

  return (
    <>
      {(error || actionError) && (
        <p className="alert error" role="alert">
          {error ?? actionError}
        </p>
      )}
      {notice && <p className="alert ok">{notice}</p>}

      <p className="sub">
        {onLoan.length} of {MAX_ACTIVE_LOANS} books out
        {remaining > 0 ? ` · you can borrow ${remaining} more` : ' · return one to borrow again'}.
        Each loan runs two weeks and can be extended once.
      </p>

      <h3 className="detail-section">Currently borrowed</h3>
      <LoansTable
        loans={onLoan}
        loading={loading && !data}
        busyId={busyId}
        onReturn={(loan) =>
          act(loan, () => transactionsApi.returnBook(loan.bookId), 'Returned. Thank you!')
        }
        onExtend={(loan) =>
          act(loan, () => transactionsApi.extend(loan.transactionId), 'Extended by two weeks.')
        }
        emptyTitle="Nothing on loan"
        emptyBody="Books you borrow show up here, with the date they are due back."
      />

      <h3 className="detail-section">Borrowing history</h3>
      <LoansTable
        loans={loans.filter((loan) => loan.returnDate)}
        loading={loading && !data}
        emptyTitle="No past loans"
        emptyBody="Once you return a book it moves down here."
      />
    </>
  )
}

/** A card row: what the setting is on the left, the control that changes it on the right. */
function SettingRow({
  label,
  help,
  children,
}: {
  label: string
  help?: string
  children?: ReactNode
}) {
  return (
    <div className="setting-row">
      <div className="setting-text">
        <span className="setting-label">{label}</span>
        {help && <span className="setting-help">{help}</span>}
      </div>
      <div className="setting-control">{children}</div>
    </div>
  )
}

type SaveState = 'idle' | 'saving' | 'saved' | 'failed'

/**
 * Reminders. The switch commits on flip rather than behind a Save button - a toggle that needs
 * confirming is the thing that left members unsure whether their choice had taken.
 */
function NotificationsCard() {
  const [enabled, setEnabled] = useState(false)
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(true)
  const [state, setState] = useState<SaveState>('idle')
  const [error, setError] = useState<string | null>(null)
  const clearSaved = useRef<number | undefined>(undefined)

  useEffect(() => {
    let cancelled = false
    remindersApi
      .mine()
      .then((setting) => {
        if (cancelled) return
        setEnabled(setting.enabled)
        setEmail(setting.email)
      })
      .catch(() => {
        // No preference stored yet is the normal first visit, not an error worth showing.
      })
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
      window.clearTimeout(clearSaved.current)
    }
  }, [])

  async function toggle(next: boolean) {
    // Optimistic: the switch moves under the finger and rolls back only if the save fails.
    setEnabled(next)
    setState('saving')
    setError(null)
    window.clearTimeout(clearSaved.current)

    try {
      const result = await remindersApi.save(next)
      setEnabled(result.enabled)
      setEmail(result.email)
      setState('saved')
      clearSaved.current = window.setTimeout(() => setState('idle'), 2500)
    } catch (err) {
      setEnabled(!next)
      setState('failed')
      setError(err instanceof Error ? err.message : 'Could not save that. Try again.')
    }
  }

  return (
    <section className="settings-card" id="notifications" aria-labelledby="notifications-title">
      <header className="settings-card-head">
        <h3 id="notifications-title">Notifications</h3>
        <p>How the library gets in touch about your loans.</p>
      </header>

      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}

      <SettingRow
        label="Due-date reminders"
        help="An email a few days before a book is due, while there is still time to return or extend it."
      >
        {loading ? (
          <span className="muted small">Loading…</span>
        ) : (
          <div className="toggle-group">
            <span className="save-state" aria-live="polite">
              {state === 'saving' && 'Saving…'}
              {state === 'saved' && 'Saved'}
            </span>
            <button
              type="button"
              role="switch"
              aria-checked={enabled}
              aria-label="Due-date reminders"
              className={enabled ? 'switch is-on' : 'switch'}
              onClick={() => toggle(!enabled)}
              disabled={state === 'saving'}
            >
              <span className="switch-thumb" />
            </button>
            <span className="switch-state">{enabled ? 'On' : 'Off'}</span>
          </div>
        )}
      </SettingRow>

      <SettingRow
        label="Sent to"
        help="Reminders go to the address on your membership. Ask the library desk to change it."
      >
        <span className="setting-value mono">{email || '—'}</span>
      </SettingRow>
    </section>
  )
}

function ProfileCard({ profile }: { profile: Profile | null }) {
  return (
    <section className="settings-card" id="profile" aria-labelledby="profile-title">
      <header className="settings-card-head">
        <h3 id="profile-title">Profile</h3>
        <p>Who the library has on record for this account.</p>
      </header>

      {!profile ? (
        <p className="muted small">Loading…</p>
      ) : (
        <>
          <SettingRow label="Username">
            <span className="setting-value mono">{profile.username}</span>
          </SettingRow>

          {profile.member && (
            <SettingRow label="Name">
              <span className="setting-value">{profile.name || '—'}</span>
            </SettingRow>
          )}

          {profile.member && (
            <SettingRow label="Email">
              <span className="setting-value mono">{profile.email || '—'}</span>
            </SettingRow>
          )}

          <SettingRow label="Account type">
            <span className="badge accent">
              {profile.role === 'ADMIN' ? 'Administrator' : 'Member'}
            </span>
          </SettingRow>

          {profile.member && (
            <SettingRow label="Books out" help={`You may hold ${profile.loanLimit} at a time.`}>
              <span className="setting-value">
                {profile.activeLoans ?? 0} of {profile.loanLimit}
              </span>
            </SettingRow>
          )}
        </>
      )}
    </section>
  )
}

function SecurityCard() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setNotice(null)

    if (next !== confirm) {
      setError('The two new passwords do not match.')
      return
    }

    setSaving(true)
    try {
      const result = await authApi.changePassword(current, next)
      setNotice(result.message)
      setCurrent('')
      setNext('')
      setConfirm('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not change your password')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="settings-card" id="security" aria-labelledby="security-title">
      <header className="settings-card-head">
        <h3 id="security-title">Security</h3>
        <p>Change the password you sign in with.</p>
      </header>

      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}
      {notice && <p className="alert ok">{notice}</p>}

      <form className="settings-form" onSubmit={submit}>
        <div className="field">
          <label htmlFor="current">Current password</label>
          <input
            id="current"
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            required
          />
        </div>

        <div className="field">
          <label htmlFor="next">New password</label>
          <input
            id="next"
            type="password"
            autoComplete="new-password"
            minLength={6}
            value={next}
            onChange={(e) => setNext(e.target.value)}
            required
          />
          <span className="help">At least 6 characters.</span>
        </div>

        <div className="field">
          <label htmlFor="confirm">Repeat new password</label>
          <input
            id="confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />
        </div>

        <button className="btn btn-primary" type="submit" disabled={saving}>
          {saving ? 'Changing…' : 'Change password'}
        </button>
      </form>
    </section>
  )
}

/**
 * Everything on one scrolling page with a rail beside it, rather than settings hidden behind
 * tabs: a member can see what the library can do before deciding to change any of it, and the
 * rail keeps its place as they scroll.
 */
function SettingsRail({ sections }: { sections: { id: string; label: string }[] }) {
  const [active, setActive] = useState(sections[0]?.id)

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]
        if (visible) setActive(visible.target.id)
      },
      // Top third of the viewport: the section the reader is looking at, not the one scrolling out.
      { rootMargin: '-80px 0px -66% 0px' },
    )

    sections.forEach(({ id }) => {
      const element = document.getElementById(id)
      if (element) observer.observe(element)
    })
    return () => observer.disconnect()
  }, [sections])

  return (
    <nav className="settings-rail" aria-label="Settings sections">
      {sections.map(({ id, label }) => (
        <a
          key={id}
          href={`#${id}`}
          className={active === id ? 'is-active' : undefined}
          aria-current={active === id ? 'true' : undefined}
          onClick={(event) => {
            event.preventDefault()
            document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
            setActive(id)
          }}
        >
          {label}
        </a>
      ))}
    </nav>
  )
}

export function AccountPage() {
  const { session } = useAuth()
  const { tab } = useParams()
  const [profile, setProfile] = useState<Profile | null>(null)

  // Staff accounts hold no membership and cannot borrow, so loans are hidden for them entirely
  // and the account page is only its settings.
  const canBorrow = Boolean(session?.customerId)
  const showSettings = tab === 'settings' || !canBorrow

  useEffect(() => {
    if (!showSettings) return
    let cancelled = false
    authApi
      .profile()
      .then((result) => !cancelled && setProfile(result))
      .catch(() => {
        // The cards fall back to the session, which is enough to render the page.
      })
    return () => {
      cancelled = true
    }
  }, [showSettings])

  const sections = canBorrow
    ? [
        { id: 'profile', label: 'Profile' },
        { id: 'notifications', label: 'Notifications' },
        { id: 'security', label: 'Security' },
      ]
    : [
        { id: 'profile', label: 'Profile' },
        { id: 'security', label: 'Security' },
      ]

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>My account</h2>
          <p className="sub">Signed in as {session?.username}</p>
        </div>
      </header>

      {canBorrow && (
        <div className="tabs">
          <NavLink to="/account" end>
            My loans
          </NavLink>
          <NavLink to="/account/settings">Settings</NavLink>
        </div>
      )}

      {showSettings ? (
        <div className="settings-layout">
          <SettingsRail sections={sections} />

          <div className="settings-cards">
            <ProfileCard profile={profile} />
            {canBorrow && <NotificationsCard />}
            <SecurityCard />
          </div>
        </div>
      ) : (
        <LoansTab />
      )}
    </section>
  )
}
