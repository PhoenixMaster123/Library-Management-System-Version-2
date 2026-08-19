package springboot.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import springboot.analytics.model.BookStat;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookStatRepository extends JpaRepository<BookStat, UUID> {

    @Query("SELECT s FROM BookStat s ORDER BY s.timesBorrowed DESC, s.title ASC")
    List<BookStat> findMostBorrowed();

    @Query("SELECT COALESCE(SUM(s.timesBorrowed), 0) FROM BookStat s")
    long totalBorrows();

    @Query("SELECT COALESCE(SUM(s.timesReturned), 0) FROM BookStat s")
    long totalReturns();
}
