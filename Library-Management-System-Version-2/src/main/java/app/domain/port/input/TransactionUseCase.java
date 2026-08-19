package app.domain.port.input;

import app.domain.dto.CreateNewTransaktion;
import app.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


/** What the application can be asked to do with loans. */
public interface TransactionUseCase {
    Transaction createNewTransaction(CreateNewTransaktion newTransaktion);
    String returnBook(UUID bookId);
    Transaction borrowBook(UUID customerId, UUID bookId);
    Transaction extendLoan(UUID transactionId);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Page<Transaction> viewAllLoans(Pageable pageable);
    Page<Transaction> viewActiveLoans(Pageable pageable);
    Optional<Transaction> findById(UUID transactionId);

    /** The loan a book is currently out on, or empty when it is on the shelf. */
    Optional<Transaction> findActiveLoanForBook(UUID bookId);

    void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate);
    void returnBookWithDates(UUID bookId, LocalDate returnDate);
}
