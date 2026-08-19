import { useEffect, useState } from 'react'
import { customersApi } from '../api/services'
import type { Customer, Page } from '../types/domain'
import { useApiCall } from '../hooks/useApiCall'
import { CustomerDetail } from '../components/CustomerDetail'
import { EmptyState, Pagination, SearchBox, SkeletonRows } from '../components/TableStates'

const PAGE_SIZE = 10
const COLUMNS = 3

export function CustomersPage() {
  const [term, setTerm] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const { data, error, loading, run } = useApiCall<Page<Customer>>()

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
  }, [query, page, run])

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
        <span className="count">{data?.totalItems != null && `${data.totalItems} total`}</span>
      </header>

      <SearchBox value={term} onChange={setTerm} placeholder="Search members by name or email" />

      {error && (
        <p className="alert error" role="alert">
          {error}
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

      {selectedId && (
        <CustomerDetail customerId={selectedId} onClose={() => setSelectedId(null)} />
      )}
    </section>
  )
}
