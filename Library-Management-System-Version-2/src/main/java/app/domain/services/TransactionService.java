package app.domain.services;

import app.domain.dto.CreateNewTransaktion;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.TransactionRepositoryPort;
import app.domain.port.input.TransactionUseCase;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionService implements TransactionUseCase {
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BookRepositoryPort bookRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;

    @Autowired
    public TransactionService(TransactionRepositoryPort transactionRepositoryPort, BookRepositoryPort bookRepositoryPort, CustomerRepositoryPort customerRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.bookRepositoryPort = bookRepositoryPort;
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public Transaction createNewTransaction(CreateNewTransaktion newTransaktion) {

        if (newTransaktion.getBorrowDate().isAfter(newTransaktion.getDueDate())) {
            throw new IllegalArgumentException("Borrow date must be before due date");
        }
        if (newTransaktion.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }

        Customer customer = customerRepositoryPort.getCustomer(newTransaktion.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Book book = bookRepositoryPort.searchBookById(newTransaktion.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));

        Transaction transaction = new Transaction(
                newTransaktion.getBorrowDate(),
                newTransaktion.getDueDate(),
                customer,
                book
        );

        transactionRepositoryPort.saveTransaction(transaction);
        return transaction;
    }

    @Override
    public String returnBook(UUID bookId) {
        List<Transaction> transactions = transactionRepositoryPort.getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        Transaction transaction = transactions.getFirst();
        transaction.setReturnDate(LocalDate.now());
        transaction.getBook().setAvailable(true);

        transactionRepositoryPort.updateTransaction(transaction);
        bookRepositoryPort.updateBook(transaction.getBook().getBookId(), transaction.getBook());

        return transaction.getTransactionId().toString();
    }

    @Override
    public Transaction borrowBook(UUID customerId, UUID bookId) {
        Book book = bookRepositoryPort.searchBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found."));

        if (!book.isAvailable()) {
            throw new RuntimeException("Book is not available for borrowing.");
        }

        Customer customer = customerRepositoryPort.getCustomer(customerId)
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

        transactionRepositoryPort.saveTransaction(transaction);

        book.setAvailable(false);
        bookRepositoryPort.updateBook(bookId, book);
        return transaction;
    }

    @Override
    public Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable) {
        return transactionRepositoryPort.viewBorrowingHistory(customerId, pageable);
    }

    @Override
    //@Cacheable(value = "transaction", key = "#transactionId")
    public Optional<Transaction> findById(UUID transactionId) {
        return transactionRepositoryPort.findTransactionById(transactionId);
    }

    @Override
    public void borrowBookWithDates(UUID customerId, UUID bookId, LocalDate borrowDate) {
        Book book = bookRepositoryPort.searchBookById(bookId)
                .orElseThrow(() -> new IllegalStateException("Book not found"));

        if (!book.isAvailable()) {
            throw new IllegalArgumentException("Book is already borrowed");
        }

        Customer customer = customerRepositoryPort.getCustomer(customerId)
                .orElseThrow(() -> new IllegalStateException("Customer not found"));

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setBorrowDate(borrowDate);
        transaction.setDueDate(borrowDate.plusWeeks(2));
        transaction.setCustomer(customer);
        transaction.setBook(book);

        book.setAvailable(false);
        bookRepositoryPort.updateBook(bookId, book);
        transactionRepositoryPort.saveTransaction(transaction);
    }

    @Override
    public void returnBookWithDates(UUID bookId, LocalDate returnDate) {
        List<Transaction> transactions = transactionRepositoryPort.getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        transactions.forEach(transaction -> {
            if (transaction.getReturnDate() == null) {
                transaction.setReturnDate(returnDate);
                transaction.getBook().setAvailable(true);
                transactionRepositoryPort.updateTransaction(transaction);
                bookRepositoryPort.updateBook(transaction.getBook().getBookId(), transaction.getBook());
                System.out.println("Returned book for transaction: " + transaction.getTransactionId());
            }
        });
    }
}
