package springboot.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springboot.analytics.model.BookStat;

import java.util.List;
import java.util.UUID;

/** Stores the running per-book tallies. */
@Repository
public interface BookStatRepository extends JpaRepository<BookStat, UUID> {

    /** Every book, most borrowed first, ties broken by title. */
    @Query("SELECT s FROM BookStat s ORDER BY s.timesBorrowed DESC, s.title ASC")
    List<BookStat> findMostBorrowed();

    /** Borrows across every book, or 0 when there are none. */
    @Query("SELECT COALESCE(SUM(s.timesBorrowed), 0) FROM BookStat s")
    long totalBorrows();

    /** Returns across every book, or 0 when there are none. */
    @Query("SELECT COALESCE(SUM(s.timesReturned), 0) FROM BookStat s")
    long totalReturns();
}
