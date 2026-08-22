package springboot.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Running totals for one book, built up from the events seen so far. */
@Entity
@Table(name = "book_stats")
@Getter
@Setter
@NoArgsConstructor
public class BookStat {

    @Id
    private UUID bookId;

    @Column(nullable = false)
    private String title;

    private String isbn;

    @Column(name = "times_borrowed", nullable = false)
    private long timesBorrowed;

    @Column(name = "times_returned", nullable = false)
    private long timesReturned;

    @Column(name = "last_activity")
    private Instant lastActivity;

    /** Starts a fresh tally for a book the service has not seen before. */
    public BookStat(UUID bookId, String title, String isbn) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
    }

    /** Borrowed minus returned: how many copies of this title are out right now. */
    public long getCurrentlyOut() {
        return Math.max(0, timesBorrowed - timesReturned);
    }
}
