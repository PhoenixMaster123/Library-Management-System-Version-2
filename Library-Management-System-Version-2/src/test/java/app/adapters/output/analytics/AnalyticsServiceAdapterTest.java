package app.adapters.output.analytics;

import app.domain.model.LoanStatistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AnalyticsServiceAdapterTest {

    @Mock
    private AnalyticsFeignClient client;

    @InjectMocks
    private AnalyticsServiceAdapter adapter;

    private void enabled(boolean value) {
        ReflectionTestUtils.setField(adapter, "analyticsEnabled", value);
    }

    @Test
    void readsSummaryAndRankedBooks() {
        enabled(true);
        UUID bookId = UUID.randomUUID();
        when(client.summary()).thenReturn(new SummaryResponse(3, 10, 6, 4, true, Instant.parse("2026-08-18T10:00:00Z")));
        when(client.popularBooks(5)).thenReturn(List.of(
                new PopularBookResponse(bookId, "Dune", "978-0-441-01359-3", 7, 5, 2)));

        Optional<LoanStatistics> result = adapter.fetch(5);

        assertThat(result).isPresent();
        LoanStatistics statistics = result.orElseThrow();
        assertThat(statistics.booksTracked()).isEqualTo(3);
        assertThat(statistics.totalBorrows()).isEqualTo(10);
        assertThat(statistics.totalReturns()).isEqualTo(6);
        assertThat(statistics.currentlyOut()).isEqualTo(4);
        assertThat(statistics.popularBooks()).singleElement().satisfies(book -> {
            assertThat(book.bookId()).isEqualTo(bookId);
            assertThat(book.title()).isEqualTo("Dune");
            assertThat(book.isbn()).isEqualTo("978-0-441-01359-3");
            assertThat(book.timesBorrowed()).isEqualTo(7);
            assertThat(book.timesReturned()).isEqualTo(5);
            assertThat(book.currentlyOut()).isEqualTo(2);
        });
    }

    /** The whole point of the port: a failure must not read as "the library lent nothing". */
    @Test
    void returnsEmptyWhenTheServiceIsUnreachable() {
        enabled(true);
        when(client.summary()).thenThrow(new RuntimeException("connection refused"));

        assertThat(adapter.fetch(10)).isEmpty();
    }

    @Test
    void returnsEmptyWhenRankedBooksFail() {
        enabled(true);
        when(client.summary()).thenReturn(new SummaryResponse(1, 1, 0, 1, true, null));
        when(client.popularBooks(anyInt())).thenThrow(new RuntimeException("read timed out"));

        assertThat(adapter.fetch(10)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheSummaryIsMissing() {
        enabled(true);
        when(client.summary()).thenReturn(null);

        assertThat(adapter.fetch(10)).isEmpty();
        verify(client, never()).popularBooks(anyInt());
    }

    @Test
    void doesNotCallTheServiceWhenDisabled() {
        enabled(false);

        assertThat(adapter.fetch(10)).isEmpty();
        verifyNoInteractions(client);
    }

    /**
     * A healthy service with no broker behind it. The adapter must pass that through rather than
     * flatten it into "zero", which is the whole reason the field exists.
     */
    @Test
    void carriesThroughThatTheStreamIsNotConnected() {
        enabled(true);
        when(client.summary()).thenReturn(new SummaryResponse(0, 0, 0, 0, false, null));
        when(client.popularBooks(10)).thenReturn(List.of());

        LoanStatistics statistics = adapter.fetch(10).orElseThrow();

        assertThat(statistics.streamConnected()).isFalse();
        assertThat(statistics.lastEventAt()).isNull();
    }

    @Test
    void carriesThroughWhenTheStreamIsHealthy() {
        enabled(true);
        Instant seen = Instant.parse("2026-08-18T10:00:00Z");
        when(client.summary()).thenReturn(new SummaryResponse(1, 2, 1, 1, true, seen));
        when(client.popularBooks(10)).thenReturn(List.of());

        LoanStatistics statistics = adapter.fetch(10).orElseThrow();

        assertThat(statistics.streamConnected()).isTrue();
        assertThat(statistics.lastEventAt()).isEqualTo(seen);
    }

    @Test
    void toleratesAnEmptyRanking() {
        enabled(true);
        when(client.summary()).thenReturn(new SummaryResponse(0, 0, 0, 0, false, null));
        when(client.popularBooks(10)).thenReturn(List.of());

        assertThat(adapter.fetch(10)).isPresent()
                .get()
                .extracting(LoanStatistics::popularBooks)
                .satisfies(books -> assertThat((List<?>) books).isEmpty());
    }
}
