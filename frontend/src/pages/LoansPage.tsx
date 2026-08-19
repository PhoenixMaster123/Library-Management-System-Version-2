import { useCallback, useEffect, useState } from 'react'
import { transactionsApi } from '../api/services'
import { LoansTable } from '../components/LoansTable'
import { Pagination } from '../components/TableStates'
import { ErrorNotice } from '../components/ErrorNotice'
import { useApiCall } from '../hooks/useApiCall'
import type { Page, Transaction } from '../types/domain'

const PAGE_SIZE = 20

/** Who has what out and when it is due, across every member. */
export function LoansPage() {
  const [activeOnly, setActiveOnly] = useState(true)
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<Page<Transaction>>()

  const refresh = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    setPage(0)
  }, [activeOnly])

  useEffect(() => {
    run(() => transactionsApi.allLoans(activeOnly, page, PAGE_SIZE))
  }, [activeOnly, page, reloadKey, run])

  async function act(loan: Transaction, action: () => Promise<unknown>) {
    setBusyId(loan.transactionId)
    setActionError(null)
    try {
      await action()
      refresh()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'That did not work')
    } finally {
      setBusyId(null)
    }
  }

  const loans = data?.data ?? []
  const totalPages = data?.totalPages ?? 0
  const overdue = loans.filter((loan) => !loan.returnDate && new Date(loan.dueDate) < new Date())

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>
            Loans <span className="badge role">Admin only</span>
          </h2>
          <p className="sub">Every book currently out, who has it, and when it is due back.</p>
        </div>
        <div className="head-actions">
          <span className="count">
            {data?.totalItems != null && `${data.totalItems} total`}
            {overdue.length > 0 && ` · ${overdue.length} overdue on this page`}
          </span>
        </div>
      </header>

      <div className="toolbar">
        <label className="checkbox">
          <input
            type="checkbox"
            checked={activeOnly}
            onChange={(event) => setActiveOnly(event.target.checked)}
          />
          Only loans still out
        </label>
      </div>

      {(error || actionError) && (
        <ErrorNotice
          message={(error ?? actionError) as string}
          onRetry={() => {
            setActionError(null)
            refresh()
          }}
        />
      )}

      <LoansTable
        loans={loans}
        loading={loading && !data}
        showBorrower
        onReturn={activeOnly ? (loan) => act(loan, () => transactionsApi.returnBook(loan.bookId)) : undefined}
        onExtend={activeOnly ? (loan) => act(loan, () => transactionsApi.extend(loan.transactionId)) : undefined}
        busyId={busyId}
        emptyTitle={activeOnly ? 'Nothing is out' : 'No loans recorded'}
        emptyBody={
          activeOnly
            ? 'Every copy is on the shelf. Untick the filter to see past loans.'
            : 'Loans appear here as soon as a member borrows a book.'
        }
      />

      <Pagination
        page={page}
        hasPrevious={page > 0}
        hasNext={page + 1 < totalPages}
        label={`Page ${page + 1} of ${Math.max(1, totalPages)}`}
        onChange={setPage}
      />
    </section>
  )
}
