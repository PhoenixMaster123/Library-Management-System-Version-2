package app.adapters.output.analytics;

import app.domain.model.LoanStatistics;
import app.domain.port.output.LoanStatisticsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads statistics from Analytics-Service, answering empty when it cannot be reached. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceAdapter implements LoanStatisticsPort {

    private final AnalyticsFeignClient client;

    @Value("${analytics.enabled:true}")
    private boolean analyticsEnabled;

    @Override
    public Optional<LoanStatistics> fetch(int limit) {
        if (!analyticsEnabled) {
            log.debug("Analytics is disabled; not calling Analytics-Service.");
            return Optional.empty();
        }

        try {
            SummaryResponse summary = client.summary();
            if (summary == null) {
                log.warn("Analytics-Service returned no summary.");
                return Optional.empty();
            }

            List<PopularBookResponse> popular = client.popularBooks(limit);

            return Optional.of(new LoanStatistics(
                    summary.booksTracked(),
                    summary.totalBorrows(),
                    summary.totalReturns(),
                    summary.currentlyOut(),
                    summary.streamConnected(),
                    summary.lastEventAt(),
                    toBookStats(popular)));
        } catch (Exception e) {
            // Unreachable, timing out, or answering with something unexpected all mean the same
            // thing to the caller: there are no statistics to show right now.
            log.warn("Could not read statistics from Analytics-Service: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static List<LoanStatistics.BookStat> toBookStats(List<PopularBookResponse> popular) {
        if (popular == null) {
            return List.of();
        }

        return popular.stream()
                .filter(Objects::nonNull)
                .map(book -> new LoanStatistics.BookStat(
                        book.bookId(),
                        book.title(),
                        book.isbn(),
                        book.timesBorrowed(),
                        book.timesReturned(),
                        book.currentlyOut()))
                .toList();
    }
}
