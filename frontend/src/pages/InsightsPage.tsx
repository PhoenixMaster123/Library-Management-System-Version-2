import { useEffect, useState } from 'react'
import { DEMO_MODE } from '../api/client'
import { analyticsApi } from '../api/services'
import { EmptyState, SkeletonRows } from '../components/TableStates'
import { ErrorNotice } from '../components/ErrorNotice'
import { useApiCall } from '../hooks/useApiCall'
import type { LoanStatistics } from '../types/domain'

const RANKED_BOOKS = 10

const TILES = [
  { key: 'booksTracked', label: 'Books tracked', hint: 'Books the event stream has seen' },
  { key: 'totalBorrows', label: 'Borrows', hint: 'Loans started, all time' },
  { key: 'totalReturns', label: 'Returns', hint: 'Loans closed, all time' },
  { key: 'currentlyOut', label: 'Currently out', hint: 'Borrowed and not yet back' },
] as const

/**
 * Borrowing statistics from Analytics-Service.
 *
 * `data === null` means the service could not be read - deliberately distinct from a library that
 * has genuinely lent nothing, which is what zeroed tiles would imply. See `analyticsApi.overview`.
 */
export function InsightsPage() {
  const [reloadKey, setReloadKey] = useState(0)
  const { data, error, loading, run } = useApiCall<LoanStatistics | null>()

  useEffect(() => {
    run(() => analyticsApi.overview(RANKED_BOOKS))
  }, [reloadKey, run])

  const books = data?.popularBooks ?? []

  // Three distinct states, and conflating any two of them tells the reader something untrue:
  //   unavailable  - Analytics-Service could not be reached at all
  //   disconnected - it answered, but it is not attached to the event stream, so it holds nothing
  //   empty        - it is attached and has genuinely seen nothing borrowed
  const unavailable = !loading && !error && data === null
  const disconnected = !loading && !!data && !data.summary.streamConnected
  const lastEvent = data?.summary.lastEventAt

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>
            Insights <span className="badge role">Admin only</span>
          </h2>
          <p className="sub">How the library is being used, across every loan on record.</p>
        </div>
      </header>

      {error && <ErrorNotice message={error} onRetry={() => setReloadKey((key) => key + 1)} />}

      {unavailable && (
        <EmptyState title="Analytics-Service isn't running">
          Statistics appear here once it is. Borrowing and returning work either way — the library
          publishes events whether or not anything is listening.
        </EmptyState>
      )}

      {disconnected && (
        <p className="alert warn" role="status">
          <span>
            <strong>These figures are not up to date.</strong> Analytics-Service is running, but it
            is not receiving events — the message broker on port 9094 does not appear to be
            reachable, so nothing the library has done since{' '}
            {lastEvent ? new Date(lastEvent).toLocaleString() : 'it started'} has reached it.
          </span>
        </p>
      )}

      {!unavailable && !error && (
        <>
          <div className="stat-grid">
            {TILES.map((tile) => (
              <div className="stat" key={tile.key}>
                <span className="stat-label">{tile.label}</span>
                <strong className="stat-value">
                  {loading || !data ? <span className="skeleton stat-skeleton" /> : data.summary[tile.key]}
                </strong>
                <small className="stat-hint">{tile.hint}</small>
              </div>
            ))}
          </div>

          <h3 className="section-title">Most borrowed</h3>

          <div className="table-wrap">
            <div className="table-scroll">
              <table className="stats-table">
                <thead>
                  <tr>
                    <th className="col-rank" aria-label="Rank" />
                    <th className="col-title">Title</th>
                    <th className="col-isbn">ISBN</th>
                    <th className="col-num">Borrowed</th>
                    <th className="col-num">Returned</th>
                    <th className="col-num">Out</th>
                  </tr>
                </thead>
                <tbody>
                  {loading && <SkeletonRows columns={6} />}

                  {!loading &&
                    books.map((book, index) => (
                      <tr key={book.bookId}>
                        <td className="rank">{index + 1}</td>
                        <td className="cell-title">
                          <span className="cell-clamp">{book.title}</span>
                        </td>
                        <td className="mono">{book.isbn || '—'}</td>
                        <td className="num">{book.timesBorrowed}</td>
                        <td className="num">{book.timesReturned}</td>
                        <td className="num">{book.currentlyOut}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            {!loading && books.length === 0 && (
              <EmptyState title={disconnected ? 'No events have arrived' : 'Nothing borrowed yet'}>
                {disconnected
                  ? 'This is not the same as nothing having been borrowed — with the broker down, no loan can reach this page.'
                  : 'Statistics build up as members borrow and return books.'}
              </EmptyState>
            )}
          </div>

          <p className="footnote">
            {DEMO_MODE
              ? 'Figures are counted from the loans in this browser. With a real backend they come from Analytics-Service instead.'
              : 'Figures come from Analytics-Service, which rebuilds them from the library.loans event stream on every start.'}
          </p>
        </>
      )}
    </section>
  )
}
