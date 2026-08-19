package app.domain.port.output;

import app.domain.model.LoanStatistics;

import java.util.Optional;

/**
 * Reads borrowing statistics from whoever is keeping them.
 *
 * <p>An empty result means they could not be read, never that there are none.
 */
public interface LoanStatisticsPort {

    /**
     * @param limit how many books to rank
     * @return the statistics, or empty when they could not be read - never empty to mean "zero"
     */
    Optional<LoanStatistics> fetch(int limit);
}
