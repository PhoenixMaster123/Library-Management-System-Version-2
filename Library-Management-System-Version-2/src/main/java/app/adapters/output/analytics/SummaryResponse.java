package app.adapters.output.analytics;

import java.time.Instant;

/**
 * Wire format of Analytics-Service's {@code GET /api/v1/analytics/summary}.
 * Adapter-local on purpose: the domain must not know this shape.
 */
public record SummaryResponse(
        long booksTracked,
        long totalBorrows,
        long totalReturns,
        long currentlyOut,
        boolean streamConnected,
        Instant lastEventAt) {
}
