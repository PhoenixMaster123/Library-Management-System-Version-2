package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {
    void saveTransaction(Transaction transaction);
    void updateTransaction(Transaction transaction);
    List<Transaction> getTransactionsForBook(Book book);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Optional<Transaction> findTransactionById(UUID transactionId);
}
