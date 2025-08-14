package app.domain.services.impl;

import app.adapters.in.dto.CreateNewTransaktion;
import app.domain.models.Book;
import app.domain.models.Customer;
import app.domain.models.Transaction;
import app.domain.port.BookDao;
import app.domain.port.CustomerDao;
import app.domain.port.TransactionDao;
import app.domain.services.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionDao transactionDao;
    private final BookDao bookDao;
    private final CustomerDao customerDao;

    public TransactionServiceImpl(TransactionDao transactionDao, BookDao bookDao, CustomerDao customerDao) {
        this.transactionDao = transactionDao;
        this.bookDao = bookDao;
        this.customerDao = customerDao;
    }
    public Transaction createNewTransaction(CreateNewTransaktion newTransaktion) {

        if (newTransaktion.getBorrowDate().isAfter(newTransaktion.getDueDate())) {
            throw new IllegalArgumentException("Borrow date must be before due date");
        }
        if (newTransaktion.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }

        Customer customer = customerDao.getCustomer(newTransaktion.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Book book = bookDao.searchBookById(newTransaktion.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));

        Transaction transaction = new Transaction(
                newTransaktion.getBorrowDate(),
                newTransaktion.getDueDate(),
                customer,
                book
        );

        transactionDao.addTransaction(transaction);
        return transaction;
    }

    public String returnBook(UUID bookId) {
        List<Transaction> transactions = transactionDao.getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        Transaction transaction = transactions.getFirst();
        transaction.setReturnDate(LocalDate.now());
        transaction.getBook().setAvailable(true);

        transactionDao.updateTransaction(transaction);
        bookDao.updateBook(transaction.getBook().getBookId(), transaction.getBook());

        return transaction.getTransactionId().toString();
    }
    public Transaction borrowBook(UUID customerId, UUID bookId) {
        Book book = bookDao.searchBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found."));

        if (!book.isAvailable()) {
            throw new RuntimeException("Book is not available for borrowing.");
        }

        Customer customer = customerDao.getCustomer(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));

        if (!customer.isPrivileges()) {
            throw new RuntimeException("Customer does not have borrowing privileges.");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setBorrowDate(LocalDate.now());
        transaction.setDueDate(LocalDate.now().plusWeeks(2));
        transaction.setCustomer(customer);
        transaction.setBook(book);

        transactionDao.addTransaction(transaction);

        book.setAvailable(false);
        bookDao.updateBook(bookId, book);
        return transaction;
    }
    public Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable) {
        return transactionDao.viewBorrowingHistory(customerId, pageable);
    }
    public Optional<Transaction> findById(UUID transactionId) {
        return transactionDao.findTransactionById(transactionId);
    }

    public void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate) {
        Book book = bookDao.searchBookById(bookId)
                .orElseThrow(() -> new IllegalStateException("Book not found"));

        if (!book.isAvailable()) {
            throw new IllegalArgumentException("Book is already borrowed");
        }

        Customer customer = customerDao.getCustomer(customerId)
                .orElseThrow(() -> new IllegalStateException("Customer not found"));

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setBorrowDate(borrowDate);
        transaction.setDueDate(borrowDate.plusWeeks(2));
        transaction.setCustomer(customer);
        transaction.setBook(book);

        book.setAvailable(false);
        bookDao.updateBook(bookId, book);
        transactionDao.addTransaction(transaction);
    }
    public void returnBookWithDates(UUID bookId, LocalDate returnDate) {
        List<Transaction> transactions = transactionDao.getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        transactions.forEach(transaction -> {
            if (transaction.getReturnDate() == null) {
                transaction.setReturnDate(returnDate);
                transaction.getBook().setAvailable(true);
                transactionDao.updateTransaction(transaction);
                bookDao.updateBook(transaction.getBook().getBookId(), transaction.getBook());
                System.out.println("Returned book for transaction: " + transaction.getTransactionId());
            }
        });
    }
}
