import { useEffect, useState } from 'react'
import { customersApi } from '../api/services'
import type { Customer, Page } from '../types/domain'
import { useApiCall } from '../hooks/useApiCall'
import { CustomerDetail } from '../components/CustomerDetail'
import { CustomerForm } from '../components/CustomerForm'
import { EmptyState, Pagination, SearchBox, SkeletonRows } from '../components/TableStates'

const PAGE_SIZE = 10
const COLUMNS = 4

export function CustomersPage() {
  const [term, setTerm] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<Page<Customer>>()

  const refresh = () => setReloadKey((key) => key + 1)

  async function act(customer: Customer, action: () => Promise<unknown>) {
    setBusyId(customer.customerId)
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

  async function remove(customer: Customer) {
    // Deleting takes their loan history with them, so it is worth a confirmation.
    if (!window.confirm(`Remove ${customer.name}? Their borrowing history goes too.`)) return
    await act(customer, () => customersApi.remove(customer.customerId))
  }

  useEffect(() => {
    const timer = setTimeout(() => setQuery(term.trim()), 300)
    return () => clearTimeout(timer)
  }, [term])

  useEffect(() => {
    setPage(0)
  }, [query])

  useEffect(() => {
    run(() =>
      query
        ? customersApi.search(query, page, PAGE_SIZE)
        : customersApi.paginated(page, PAGE_SIZE),
    )
  }, [query, page, reloadKey, run])

  const customers = data?.data ?? []
  const showSkeleton = loading && !data
  const totalPages = data?.totalPages ?? 0

  return (
    <section>
      <header className="page-head">
        <div>
          <h2>
            Members <span className="badge role">Admin only</span>
          </h2>
          <p className="sub">Everyone who holds a library account. Visible to administrators.</p>
        </div>
        <div className="head-actions">
          <span className="count">{data?.totalItems != null && `${data.totalItems} total`}</span>
          <button className="btn btn-primary" type="button" onClick={() => setAdding(true)}>
            New member
          </button>
        </div>
      </header>

      <SearchBox value={term} onChange={setTerm} placeholder="Search members by name or email" />

      {(error || actionError) && (
        <p className="alert error" role="alert">
          {error ?? actionError}
        </p>
      )}

      <div className="table-wrap">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Borrowing privileges</th>
                <th className="col-actions" aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {showSkeleton && <SkeletonRows columns={COLUMNS} />}

              {!showSkeleton &&
                customers.map((customer) => (
                  <tr
                    key={customer.customerId}
                    className="row-clickable"
                    tabIndex={0}
                    onClick={() => setSelectedId(customer.customerId)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        setSelectedId(customer.customerId)
                      }
                    }}
                  >
                    <td className="cell-title">{customer.name}</td>
                    <td className="mono">{customer.email}</td>
                    <td>
                      <span className={customer.privileges ? 'badge ok' : 'badge out'}>
                        {customer.privileges ? 'Active' : 'Suspended'}
                      </span>
                    </td>
                    <td className="cell-actions" onClick={(event) => event.stopPropagation()}>
                      <button
                        className="btn btn-ghost"
                        type="button"
                        disabled={busyId === customer.customerId}
                        onClick={() =>
                          act(customer, () =>
                            customersApi.setPrivileges(customer.customerId, !customer.privileges),
                          )
                        }
                      >
                        {customer.privileges ? 'Suspend' : 'Reinstate'}
                      </button>
                      <button
                        className="btn btn-ghost danger"
                        type="button"
                        disabled={busyId === customer.customerId}
                        onClick={() => remove(customer)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        {!showSkeleton && customers.length === 0 && (
          <EmptyState title={query ? 'No members match that search' : 'No members on this page'}>
            {query ? 'Try another name or email address.' : 'New members appear here when they sign up.'}
          </EmptyState>
        )}
      </div>

      <Pagination
        page={page}
        hasPrevious={page > 0}
        hasNext={page + 1 < totalPages}
        label={`Page ${page + 1} of ${Math.max(1, totalPages)}`}
        onChange={setPage}
      />

      {adding && (
        <CustomerForm
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            refresh()
          }}
        />
      )}

      {selectedId && (
        <CustomerDetail customerId={selectedId} onClose={() => setSelectedId(null)} />
      )}
    </section>
  )
}
