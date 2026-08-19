import type { ReactNode } from 'react'

/** Keeps the table height stable while a page is in flight, instead of a bare "Loading…". */
export function SkeletonRows({ rows = 5, columns }: { rows?: number; columns: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, row) => (
        <tr key={row}>
          {Array.from({ length: columns }, (_, column) => (
            <td key={column}>
              <span className="skeleton" style={{ width: `${55 + ((row + column) % 4) * 12}%` }} />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <strong>{title}</strong>
      {children}
    </div>
  )
}

export function SearchBox({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
}) {
  return (
    <div className="toolbar">
      <div className="search">
        <span className="icon">
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.2-3.2" />
          </svg>
        </span>
        <input
          type="search"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          aria-label={placeholder}
        />
      </div>
      {value && (
        <button className="btn btn-ghost" type="button" onClick={() => onChange('')}>
          Clear
        </button>
      )}
    </div>
  )
}

export function Pagination({
  page,
  hasPrevious,
  hasNext,
  label,
  onChange,
}: {
  page: number
  hasPrevious: boolean
  hasNext: boolean
  label: string
  onChange: (page: number) => void
}) {
  return (
    <div className="pagination">
      <button
        className="btn"
        type="button"
        onClick={() => onChange(Math.max(0, page - 1))}
        disabled={!hasPrevious}
      >
        ← Previous
      </button>
      <span className="page-info">{label}</span>
      <button className="btn" type="button" onClick={() => onChange(page + 1)} disabled={!hasNext}>
        Next →
      </button>
    </div>
  )
}
