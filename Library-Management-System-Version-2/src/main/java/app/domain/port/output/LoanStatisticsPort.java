package app.domain.port.output;

import app.domain.model.LoanStatistics;

import java.util.Optional;

/** Reads borrowing statistics from whoever keeps them. */
public interface LoanStatisticsPort {

    /** The statistics with limit books ranked, or empty when unreadable - never empty to mean zero. */
    Optional<LoanStatistics> fetch(int limit);
}
