package app.domain.services;

import app.domain.dto.CreateNewTransaktion;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import app.domain.port.input.TransactionUseCase;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.LoanEventPort;
import app.domain.port.output.NotificationPort;
import app.domain.port.output.TransactionRepositoryPort;
import app.infrastructure.exceptions.BookNotFoundException;
import app.infrastructure.exceptions.BorrowNotAllowedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The loan rules: who may borrow, for how long, and what a return does. */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements TransactionUseCase {

    /** How many books one member may hold at a time. */
    public static final int MAX_ACTIVE_LOANS = 3;

    /** A loan runs two weeks, and may be extended by another two weeks exactly once. */
    private static final int LOAN_WEEKS = 2;
    private static final int EXTENSION_WEEKS = 2;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BookRepositoryPort bookRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final NotificationPort notificationPort;
    private final LoanEventPort loanEventPort;

    /** Records a loan from explicit dates, checking they are ordered and in the future. */
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

    /** Closes the book's open loan, puts it back on the shelf and announces the return. */
    @Override
    public String returnBook(UUID bookId) {
        List<Transaction> transactions = transactionRepositoryPort
                .getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        // Pick the loan that is still open. Using the first transaction would re-close an
        // already-returned loan and credit the return to the wrong customer.
        Transaction transaction = transactions.stream()
                .filter(t -> t.getReturnDate() == null)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("This book has no open loan to return."));

        transaction.setReturnDate(LocalDate.now());
        transaction.getBook().setAvailable(true);

        transactionRepositoryPort.updateTransaction(transaction);
        bookRepositoryPort.updateBook(transaction.getBook().getBookId(), transaction.getBook());

        notificationPort.notifyBookReturned(transaction.getCustomer(), transaction.getBook());
        loanEventPort.bookReturned(transaction.getCustomer(), transaction.getBook());

        return transaction.getTransactionId().toString();
    }

    /** Lends a book, refusing when it is out, the member lacks privileges, or the limit is reached. */
    @Override
    public Transaction borrowBook(UUID customerId, UUID bookId) {
        // Named exceptions rather than a bare RuntimeException: the web layer has to tell a
        // missing record (404) and a broken library rule (400) apart from an actual failure (500).
        Book book = bookRepositoryPort.searchBookById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found."));

        if (!book.isAvailable()) {
            throw new BorrowNotAllowedException("Book is not available for borrowing.");
        }

        Customer customer = customerRepositoryPort.getCustomer(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));

        if (!customer.isPrivileges()) {
            throw new BorrowNotAllowedException("Customer does not have borrowing privileges.");
        }

        long onLoan = transactionRepositoryPort.countActiveLoans(customerId);
        if (onLoan >= MAX_ACTIVE_LOANS) {
            throw new BorrowNotAllowedException(
                    "You already have " + onLoan + " books out. The limit is " + MAX_ACTIVE_LOANS
                            + " at a time - return one before borrowing another.");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setBorrowDate(LocalDate.now());
        transaction.setDueDate(LocalDate.now().plusWeeks(LOAN_WEEKS));
        transaction.setCustomer(customer);
        transaction.setBook(book);

        transactionRepositoryPort.saveTransaction(transaction);

        book.setAvailable(false);
        bookRepositoryPort.updateBook(bookId, book);

        notificationPort.notifyBookBorrowed(customer, book, transaction.getDueDate());
        loanEventPort.bookBorrowed(customer, book);

        return transaction;
    }

    /** Pushes the due date out by another loan period. Allowed once, and only while the book is out. */
    @Override
    public Transaction extendLoan(UUID transactionId) {
        Transaction transaction = transactionRepositoryPort.findTransactionById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));

        if (transaction.getReturnDate() != null) {
            throw new BorrowNotAllowedException("This book has already been returned.");
        }
        if (transaction.isExtended()) {
            throw new BorrowNotAllowedException("This loan has already been extended once.");
        }

        transaction.setDueDate(transaction.getDueDate().plusWeeks(EXTENSION_WEEKS));
        transaction.setExtended(true);
        transactionRepositoryPort.updateTransaction(transaction);

        return transaction;
    }

    /** One page of one member's loans, past and present. */
    @Override
    public Page<Transaction> viewBorrowingHistory(UUID customerId, Pageable pageable) {
        return transactionRepositoryPort.viewBorrowingHistory(customerId, pageable);
    }

    /** One page of every loan in the library. */
    @Override
    public Page<Transaction> viewAllLoans(Pageable pageable) {
        return transactionRepositoryPort.findAllTransactions(pageable);
    }

    /** One page of the loans still outstanding. */
    @Override
    public Page<Transaction> viewActiveLoans(Pageable pageable) {
        return transactionRepositoryPort.findActiveLoans(pageable);
    }

    /** The loan with this id, or empty. */
    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        return transactionRepositoryPort.findTransactionById(transactionId);
    }

    /** The loan a book is out on, or empty when it is on the shelf. */
    @Override
    public Optional<Transaction> findActiveLoanForBook(UUID bookId) {
        return transactionRepositoryPort.findActiveLoanForBook(bookId);
    }

    /** Records a borrow dated in the past, for seeding and imports. Skips the loan limit. */
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

    /** Closes a book's open loans on a past date, for seeding and imports. */
    @Override
    public void returnBookWithDates(UUID bookId, LocalDate returnDate) {
        List<Transaction> transactions = transactionRepositoryPort
                .getTransactionsForBook(new Book(bookId, null, null, 0, false, null));

        if (transactions.isEmpty()) {
            throw new EntityNotFoundException("No transaction found for the given book.");
        }

        transactions.forEach(transaction -> {
            if (transaction.getReturnDate() == null) {
                transaction.setReturnDate(returnDate);
                transaction.getBook().setAvailable(true);
                transactionRepositoryPort.updateTransaction(transaction);
                bookRepositoryPort.updateBook(transaction.getBook().getBookId(), transaction.getBook());
                log.info("Returned book for transaction: {}", transaction.getTransactionId());
            }
        });
    }
}
