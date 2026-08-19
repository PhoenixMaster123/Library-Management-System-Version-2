import { useEffect, useState } from 'react'
import { customersApi, transactionsApi } from '../api/services'
import type { Customer, Transaction } from '../types/domain'
import { LoansTable } from './LoansTable'
import { Modal } from './Modal'

interface CustomerDetailProps {
  customerId: string
  onClose: () => void
}

/** A member's details together with what they have out - the two questions an admin asks at once. */
export function CustomerDetail({ customerId, onClose }: CustomerDetailProps) {
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [loans, setLoans] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([customersApi.byId(customerId), transactionsApi.history(customerId, 0, 50)])
      .then(([member, history]) => {
        if (cancelled) return
        setCustomer(member)
        setLoans(history.data)
      })
      .catch((err) => !cancelled && setError(err instanceof Error ? err.message : 'Could not load this member'))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [customerId])

  const onLoan = loans.filter((loan) => !loan.returnDate)

  return (
    <Modal title={customer?.name ?? 'Member'} onClose={onClose}>
      {error && (
        <p className="alert error" role="alert">
          {error}
        </p>
      )}

      {customer && (
        <>
          <dl className="detail-grid">
            <div>
              <dt>Email</dt>
              <dd className="mono">{customer.email}</dd>
            </div>
            <div>
              <dt>Borrowing privileges</dt>
              <dd>
                <span className={customer.privileges ? 'badge ok' : 'badge out'}>
                  {customer.privileges ? 'Active' : 'Suspended'}
                </span>
              </dd>
            </div>
            <div>
              <dt>Currently out</dt>
              <dd>{onLoan.length}</dd>
            </div>
            <div>
              <dt>Member id</dt>
              <dd className="mono">{customer.customerId}</dd>
            </div>
          </dl>

          <h4 className="detail-section">Loans</h4>
          <LoansTable
            loans={loans}
            loading={loading}
            emptyTitle="No loans yet"
            emptyBody="This member has not borrowed anything."
          />
        </>
      )}
    </Modal>
  )
}
