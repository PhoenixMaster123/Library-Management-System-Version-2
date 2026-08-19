import type { Transaction } from '../types/domain'
import { EmptyState, SkeletonRows } from './TableStates'

/** Whole days from today until `date`; negative once the date has passed. */
function daysUntil(date: string): number {
  const due = new Date(date)
  const today = new Date()
  due.setHours(0, 0, 0, 0)
  today.setHours(0, 0, 0, 0)
  return Math.round((due.getTime() - today.getTime()) / 86_400_000)
}

function DueStatus({ loan }: { loan: Transaction }) {
  if (loan.returnDate) {
    return <span className="badge ok">Returned {loan.returnDate}</span>
  }

  const days = daysUntil(loan.dueDate)
  const extended = loan.extended ? ' · extended' : ''

  if (days < 0) {
    return (
      <span className="badge out">
        Overdue by {Math.abs(days)}d{extended}
      </span>
    )
  }
  return (
    <span className="badge warn">
      {days === 0 ? 'Due today' : `Due in ${days}d`}
      {extended}
    </span>
  )
}

interface LoansTableProps {
  loans: Transaction[]
  loading: boolean
  /** Adds a borrower column - for the administrator views, where loans span members. */
  showBorrower?: boolean
  onReturn?: (loan: Transaction) => void
  onExtend?: (loan: Transaction) => void
  /** Transaction currently being acted on, so only its own buttons go quiet. */
  busyId?: string | null
  emptyTitle: string
  emptyBody: string
}

export function LoansTable({
  loans,
  loading,
  showBorrower = false,
  onReturn,
  onExtend,
  busyId,
  emptyTitle,
  emptyBody,
}: LoansTableProps) {
  const hasActions = Boolean(onReturn || onExtend)
  const columns = 4 + (showBorrower ? 1 : 0) + (hasActions ? 1 : 0)

  return (
    <div className="table-wrap">
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Book</th>
              {showBorrower && <th>Borrowed by</th>}
              <th>Borrowed</th>
              <th>Due</th>
              <th>Status</th>
              {hasActions && <th aria-label="Actions" />}
            </tr>
          </thead>
          <tbody>
            {loading && <SkeletonRows columns={columns} />}

            {!loading &&
              loans.map((loan) => (
                <tr key={loan.transactionId}>
                  <td className="cell-title">{loan.book?.title ?? '—'}</td>
                  {showBorrower && (
                    <td>
                      {loan.customer?.name ?? '—'}
                      {loan.customer?.email && <small className="muted"> · {loan.customer.email}</small>}
                    </td>
                  )}
                  <td>{loan.borrowDate}</td>
                  <td>{loan.dueDate}</td>
                  <td>
                    <DueStatus loan={loan} />
                  </td>
                  {hasActions && (
                    <td className="cell-actions">
                      {!loan.returnDate && onExtend && (
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => onExtend(loan)}
                          disabled={busyId === loan.transactionId || loan.extended}
                          title={loan.extended ? 'A loan can only be extended once' : 'Add two weeks'}
                        >
                          Extend
                        </button>
                      )}
                      {!loan.returnDate && onReturn && (
                        <button
                          className="btn"
                          type="button"
                          onClick={() => onReturn(loan)}
                          disabled={busyId === loan.transactionId}
                        >
                          {busyId === loan.transactionId ? 'Working…' : 'Return'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {!loading && loans.length === 0 && <EmptyState title={emptyTitle}>{emptyBody}</EmptyState>}
    </div>
  )
}
