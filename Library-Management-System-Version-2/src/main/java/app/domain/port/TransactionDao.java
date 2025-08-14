package app.domain.port;

import app.domain.models.Book;
import app.domain.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionDao {
    void addTransaction(Transaction transaction);
    List<Transaction> getTransactionsForBook(Book book);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Optional<Transaction> findTransactionById(UUID transactionId);
    void updateTransaction(Transaction transaction);
}
