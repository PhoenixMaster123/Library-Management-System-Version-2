interface PaginationProps {
  page: number
  totalPages: number
  totalItems: number
  pageSize: number
  onPage: (page: number) => void
  onPageSize?: (size: number) => void
  /** What is being counted, for the summary line: "books", "results". */
  unit?: string
}

const PAGE_SIZES = [25, 50, 100]

/**
 * Numbered pages rather than an endless "Load more": with a catalogue of thousands, being able to
 * jump to a page - and to come back to the one you were on - is the whole point.
 *
 * Ellipses keep the control a fixed width however many pages there are: first, last, and a window
 * of three around the current page.
 */
function pageNumbers(current: number, total: number): (number | 'gap')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index)
  }

  const numbers = new Set<number>([0, total - 1, current])
  if (current > 0) numbers.add(current - 1)
  if (current < total - 1) numbers.add(current + 1)

  const sorted = [...numbers].sort((a, b) => a - b)
  const withGaps: (number | 'gap')[] = []

  sorted.forEach((value, index) => {
    if (index > 0 && value - sorted[index - 1] > 1) withGaps.push('gap')
    withGaps.push(value)
  })

  return withGaps
}

export function Pagination({
  page,
  totalPages,
  totalItems,
  pageSize,
  onPage,
  onPageSize,
  unit = 'books',
}: PaginationProps) {
  if (totalItems === 0) return null

  const first = page * pageSize + 1
  const last = Math.min(totalItems, (page + 1) * pageSize)

  return (
    <nav className="pagination" aria-label="Pagination">
      <p className="pagination-summary">
        Showing <strong>{first.toLocaleString()}</strong>–<strong>{last.toLocaleString()}</strong> of{' '}
        <strong>{totalItems.toLocaleString()}</strong> {unit}
      </p>

      {totalPages > 1 && (
        <div className="pagination-pages">
          <button
            className="btn btn-ghost"
            type="button"
            onClick={() => onPage(page - 1)}
            disabled={page === 0}
          >
            ‹ Prev
          </button>

          {pageNumbers(page, totalPages).map((value, index) =>
            value === 'gap' ? (
              <span key={`gap-${index}`} className="pagination-gap" aria-hidden="true">
                …
              </span>
            ) : (
              <button
                key={value}
                className={value === page ? 'btn page-btn is-current' : 'btn btn-ghost page-btn'}
                type="button"
                onClick={() => onPage(value)}
                aria-current={value === page ? 'page' : undefined}
              >
                {value + 1}
              </button>
            ),
          )}

          <button
            className="btn btn-ghost"
            type="button"
            onClick={() => onPage(page + 1)}
            disabled={page >= totalPages - 1}
          >
            Next ›
          </button>
        </div>
      )}

      {onPageSize && (
        <label className="pagination-size">
          Per page
          <select value={pageSize} onChange={(event) => onPageSize(Number(event.target.value))}>
            {PAGE_SIZES.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
      )}
    </nav>
  )
}
