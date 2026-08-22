package app.adapters.output.analytics;

import java.time.Instant;

/** Wire format of Analytics-Service's summary reply. Adapter-local: the domain must not know it. */
public record SummaryResponse(
        long booksTracked,
        long totalBorrows,
        long totalReturns,
        long currentlyOut,
        boolean streamConnected,
        Instant lastEventAt) {
}
