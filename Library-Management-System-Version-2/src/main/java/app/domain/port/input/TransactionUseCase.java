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
    /** Records a loan from the submitted details. */
    Transaction createNewTransaction(CreateNewTransaktion newTransaktion);

    /** Takes a book back and reports the outcome. */
    String returnBook(UUID bookId);

    /** Lends a book to a member and returns the new loan. */
    Transaction borrowBook(UUID customerId, UUID bookId);

    /** Pushes a loan's due date back and returns the updated loan. */
    Transaction extendLoan(UUID transactionId);

    /** One page of one member's loans, past and present. */
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);

    /** One page of every loan in the library. */
    Page<Transaction> viewAllLoans(Pageable pageable);

    /** One page of the loans still outstanding. */
    Page<Transaction> viewActiveLoans(Pageable pageable);

    /** The loan with this id, or empty. */
    Optional<Transaction> findById(UUID transactionId);

    /** The loan a book is currently out on, or empty when it is on the shelf. */
    Optional<Transaction> findActiveLoanForBook(UUID bookId);

    /** Records a borrow that happened on a past date, for seeding and imports. */
    void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate);

    /** Records a return that happened on a past date, for seeding and imports. */
    void returnBookWithDates(UUID bookId, LocalDate returnDate);
}
