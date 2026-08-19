import { useCallback, useRef, useState } from 'react'
import { UnauthorizedError } from '../api/client'

interface ApiCallState<T> {
  data: T | null
  error: string | null
  loading: boolean
  run: (fn: () => Promise<T>) => Promise<void>
}

/**
 * Minimal async-state helper: loading/error/data plus a stable `run`.
 *
 * Guards against a slow response from an earlier render overwriting a newer one,
 * which is what makes rapid pagination clicks show stale rows.
 */
export function useApiCall<T>(): ApiCallState<T> {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const callId = useRef(0)

  const run = useCallback(async (fn: () => Promise<T>) => {
    const current = ++callId.current
    setLoading(true)
    setError(null)
    try {
      const result = await fn()
      if (current === callId.current) {
        setData(result)
      }
    } catch (err) {
      if (current !== callId.current) return
      if (err instanceof UnauthorizedError) {
        // RequireAuth sends the user to /login on the next render.
        window.location.assign('/login')
        return
      }
      setError(err instanceof Error ? err.message : 'Something went wrong')
    } finally {
      if (current === callId.current) {
        setLoading(false)
      }
    }
  }, [])

  return { data, error, loading, run }
}
