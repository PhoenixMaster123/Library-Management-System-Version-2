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
    void saveTransaction(Transaction transaction);
    void updateTransaction(Transaction transaction);
    List<Transaction> getTransactionsForBook(Book book);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Optional<Transaction> findTransactionById(UUID transactionId);

    /** The one loan a book is still out on. Empty when it is on the shelf. */
    Optional<Transaction> findActiveLoanForBook(UUID bookId);
    Page<Transaction> findAllTransactions(Pageable pageable);
    Page<Transaction> findActiveLoans(Pageable pageable);
    long countActiveLoans(UUID customerId);
    List<Transaction> findLoansDueOn(LocalDate dueDate);
}
