package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage the domain needs for loans. */
public interface TransactionRepositoryPort {
    /** Stores a new loan. */
    void saveTransaction(Transaction transaction);

    /** Overwrites a stored loan. */
    void updateTransaction(Transaction transaction);

    /** Every loan ever recorded against one book. */
    List<Transaction> getTransactionsForBook(Book book);

    /** One page of one member's loans. */
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);

    /** The stored loan with this id, or empty. */
    Optional<Transaction> findTransactionById(UUID transactionId);

    /** The one loan a book is still out on. Empty when it is on the shelf. */
    Optional<Transaction> findActiveLoanForBook(UUID bookId);

    /** One page of every stored loan. */
    Page<Transaction> findAllTransactions(Pageable pageable);

    /** One page of the loans still outstanding. */
    Page<Transaction> findActiveLoans(Pageable pageable);

    /** How many books a member currently has out. */
    long countActiveLoans(UUID customerId);

    /** Every loan due back on this date. */
    List<Transaction> findLoansDueOn(LocalDate dueDate);
}
