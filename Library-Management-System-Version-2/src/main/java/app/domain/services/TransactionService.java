package app.domain.services;

import app.domain.models.Book;
import app.domain.port.BookDao;
import app.domain.port.CustomerDao;
import app.domain.port.TransactionDao;
import app.adapters.in.dto.CreateNewTransaktion;
import app.domain.models.Customer;
import app.domain.models.Transaction;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TransactionService {
    Transaction createNewTransaction(CreateNewTransaktion newTransaktion);
    String returnBook(UUID bookId);
    Transaction borrowBook(UUID customerId, UUID bookId);
    Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable);
    Optional<Transaction> findById(UUID transactionId);
    void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate);
    void returnBookWithDates(UUID bookId, LocalDate returnDate);
}
