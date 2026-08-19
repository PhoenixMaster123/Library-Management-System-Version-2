import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { InsightsPage } from './InsightsPage'

const fetchMock = vi.fn()

function respond(status: number, body: unknown) {
  return new Response(typeof body === 'string' ? body : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const statistics = {
  summary: {
    booksTracked: 128,
    totalBorrows: 1432,
    totalReturns: 1289,
    currentlyOut: 143,
    streamConnected: true,
    lastEventAt: '2026-08-18T10:00:00Z',
  },
  popularBooks: [
    { bookId: '1', title: 'Dune', isbn: '978-0-441-01359-3', timesBorrowed: 87, timesReturned: 84, currentlyOut: 3 },
  ],
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('InsightsPage', () => {
  it('shows the figures it is given', async () => {
    fetchMock.mockResolvedValue(respond(200, statistics))

    render(<InsightsPage />)

    expect(await screen.findByText('128')).toBeInTheDocument()
    expect(screen.getByText('1432')).toBeInTheDocument()
    expect(screen.getByText('Dune')).toBeInTheDocument()
    expect(screen.getByText('978-0-441-01359-3')).toBeInTheDocument()
  })

  /**
   * The distinction the whole feature turns on. Analytics-Service being down must not render as a
   * library that has never lent a book.
   */
  it('says the service is down rather than showing zeros', async () => {
    fetchMock.mockResolvedValue(respond(503, { message: 'Analytics is unavailable.' }))

    render(<InsightsPage />)

    expect(await screen.findByText(/isn't running/i)).toBeInTheDocument()
    expect(screen.queryByText('Books tracked')).not.toBeInTheDocument()
    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })

  it('shows a genuine zero as a zero', async () => {
    fetchMock.mockResolvedValue(
      respond(200, {
        summary: {
          booksTracked: 0, totalBorrows: 0, totalReturns: 0, currentlyOut: 0,
          streamConnected: true, lastEventAt: null,
        },
        popularBooks: [],
      }),
    )

    render(<InsightsPage />)

    expect(await screen.findByText(/nothing borrowed yet/i)).toBeInTheDocument()
    expect(screen.queryByText(/isn't running/i)).not.toBeInTheDocument()
    expect(screen.getAllByText('0').length).toBeGreaterThan(0)
  })

  /**
   * Analytics up, broker down. The service answers 200 with zeros, and presenting those as fact
   * claims the library has never lent a book - which is exactly what a live loan disproves.
   */
  it('does not present an unfed projection as "nothing borrowed"', async () => {
    fetchMock.mockResolvedValue(
      respond(200, {
        summary: {
          booksTracked: 0, totalBorrows: 0, totalReturns: 0, currentlyOut: 0,
          streamConnected: false, lastEventAt: null,
        },
        popularBooks: [],
      }),
    )

    render(<InsightsPage />)

    expect(await screen.findByText(/not up to date/i)).toBeInTheDocument()
    expect(screen.getByText(/no events have arrived/i)).toBeInTheDocument()
    expect(screen.queryByText(/nothing borrowed yet/i)).not.toBeInTheDocument()
  })

  it('distinguishes a disconnected stream from an unreachable service', async () => {
    fetchMock.mockResolvedValue(
      respond(200, {
        summary: {
          booksTracked: 0, totalBorrows: 0, totalReturns: 0, currentlyOut: 0,
          streamConnected: false, lastEventAt: null,
        },
        popularBooks: [],
      }),
    )

    render(<InsightsPage />)

    await screen.findByText(/not up to date/i)
    expect(screen.queryByText(/isn't running/i)).not.toBeInTheDocument()
  })

  /** Broker died after events had arrived: the figures are real but stale, so show both. */
  it('shows stale figures with a warning rather than hiding them', async () => {
    fetchMock.mockResolvedValue(
      respond(200, {
        summary: {
          booksTracked: 5, totalBorrows: 40, totalReturns: 35, currentlyOut: 5,
          streamConnected: false, lastEventAt: '2026-08-18T09:00:00Z',
        },
        popularBooks: [
          { bookId: '1', title: 'Dune', isbn: '1', timesBorrowed: 9, timesReturned: 8, currentlyOut: 1 },
        ],
      }),
    )

    render(<InsightsPage />)

    expect(await screen.findByText(/not up to date/i)).toBeInTheDocument()
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByText('Dune')).toBeInTheDocument()
  })

  it('says nothing about the stream when it is healthy', async () => {
    fetchMock.mockResolvedValue(respond(200, statistics))

    render(<InsightsPage />)

    await screen.findByText('128')
    expect(screen.queryByText(/not up to date/i)).not.toBeInTheDocument()
  })

  it('reports a real failure as an error, not as "unavailable"', async () => {
    fetchMock.mockResolvedValue(respond(500, { message: 'Something went wrong on the library server.' }))

    render(<InsightsPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent(/went wrong/i)
    expect(screen.queryByText(/isn't running/i)).not.toBeInTheDocument()
  })

  it('never puts a raw status code on screen', async () => {
    fetchMock.mockResolvedValue(respond(502, ''))

    render(<InsightsPage />)

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(document.body.textContent).not.toMatch(/status 502/i)
  })

  it('asks for the statistics behind the admin endpoint', async () => {
    fetchMock.mockResolvedValue(respond(200, statistics))

    render(<InsightsPage />)

    await waitFor(() => expect(fetchMock).toHaveBeenCalled())
    expect(fetchMock.mock.calls[0][0]).toContain('/backend/admin/analytics')
  })
})
