package app.domain.port.input;

import app.domain.dto.CreateNewTransaktion;
import app.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


public interface TransactionUseCase {
    Transaction createNewTransaction(CreateNewTransaktion newTransaktion);
    String returnBook(UUID bookId);
    Transaction borrowBook(UUID customerId, UUID bookId);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Optional<Transaction> findById(UUID transactionId);
    void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate);
    void returnBookWithDates(UUID bookId, LocalDate returnDate);
}
