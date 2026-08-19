/**
 * An error the user can do something about.
 *
 * The failures that matter most here are transient - the backend restarting, a connection
 * dropping - and recovering from those used to mean reloading the page or restarting a server.
 * A retry turns that into one click, so the alert offers one wherever the caller can reload.
 */
export function ErrorNotice({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <p className="alert error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="btn btn-ghost alert-action" type="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </p>
  )
}
