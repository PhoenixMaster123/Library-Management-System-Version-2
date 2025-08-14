package app.domain.services.integrationTests;

import app.adapters.in.dto.CreateNewTransaktion;
import app.adapters.out.H2.repositories.BookRepository;
import app.adapters.out.H2.repositories.CustomerRepository;
import app.adapters.out.H2.repositories.TransactionRepository;
import app.domain.models.Book;
import app.domain.models.Customer;
import app.domain.models.Transaction;
import app.domain.port.BookDao;
import app.domain.port.CustomerDao;
import app.domain.port.TransactionDao;
import app.domain.services.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public class TransactionServiceIT {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionDao transactionDao;
    @Autowired
    private BookDao bookDao;
    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testCreateNewTransaction_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerDao.addCustomer(customer);

        customer = customerDao.getCustomer(customer.getCustomerId()).get();

        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookDao.addBook(book);

        CreateNewTransaktion createNewTransaction = new CreateNewTransaktion(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10),
                customer.getCustomerId(),
                book.getBookId()
        );

        Transaction transaction = transactionService.createNewTransaction(createNewTransaction);

        assertThat(transaction).isNotNull();
        assertThat(transaction.getCustomer()).isNotNull();
        assertThat(transaction.getBook()).isNotNull();
        assertThat(transaction.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(transaction.getBook().getBookId()).isEqualTo(book.getBookId());
        assertThat(transaction.getBorrowDate()).isEqualTo(createNewTransaction.getBorrowDate());
        assertThat(transaction.getDueDate()).isEqualTo(createNewTransaction.getDueDate());
        assertThat(transaction.getTransactionId()).isNotNull();
    }

    @Test
    void testReturnBook_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerDao.addCustomer(customer);
        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, false, LocalDate.now());
        bookDao.addBook(book);
        Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
        transactionDao.addTransaction(transaction);

        String transactionId = transactionService.returnBook(book.getBookId());

        Optional<Transaction> returnedTransaction = transactionDao.findTransactionById(UUID.fromString(transactionId));
        assertThat(returnedTransaction).isPresent();
        assertThat(returnedTransaction.get().getReturnDate()).isNotNull();
        assertThat(bookDao.searchBookById(book.getBookId()).get().isAvailable()).isTrue();
    }

    @Test
    void testBorrowBook_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerDao.addCustomer(customer);

        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookDao.addBook(book);

        Transaction transaction = transactionService.borrowBook(customer.getCustomerId(), book.getBookId());

        assertThat(transaction.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(transaction.getBook().getBookId()).isEqualTo(book.getBookId());
        assertThat(bookDao.searchBookById(book.getBookId()).get().isAvailable()).isFalse();
    }

    @Test
    void testViewBorrowingHistory_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerDao.addCustomer(customer);
        Book book1 = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookDao.addBook(book1);
        Book book2 = new Book(UUID.randomUUID(), "Design Patterns", "Gang of Four", 1994, true, LocalDate.now());
        bookDao.addBook(book2);

        transactionService.borrowBook(customer.getCustomerId(), book1.getBookId());
        transactionService.borrowBook(customer.getCustomerId(), book2.getBookId());

        Pageable pageable = PageRequest.of(0, 10);

        Page<Transaction> actualPage = transactionService.viewBorrowingHistory(customer.getCustomerId(), pageable);

        assertThat(actualPage.getContent()).hasSize(2);
    }

    @Test
    void testFindById_Integration() {
        Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
        customerDao.addCustomer(customer);
        Book book = new Book(null, "Clean Code", "Robert C. Martin", 2008, false, LocalDate.now());
        bookDao.addBook(book);
        Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
        transactionDao.addTransaction(transaction);

        Optional<Transaction> actualTransaction = transactionService.findById(transaction.getTransactionId());

        assertThat(actualTransaction).isPresent();

        Transaction actual = actualTransaction.get();
        assertThat(actual.getTransactionId()).isEqualTo(transaction.getTransactionId());
        assertThat(actual.getBorrowDate()).isEqualTo(transaction.getBorrowDate());
        assertThat(actual.getDueDate()).isEqualTo(transaction.getDueDate());

        assertThat(actual.getCustomer().getCustomerId()).isEqualTo(transaction.getCustomer().getCustomerId());
        assertThat(actual.getCustomer().getName()).isEqualTo(transaction.getCustomer().getName());
        assertThat(actual.getCustomer().getEmail()).isEqualTo(transaction.getCustomer().getEmail());
        assertThat(actual.getCustomer().isPrivileges()).isEqualTo(transaction.getCustomer().isPrivileges());

        assertThat(actual.getBook().getBookId()).isEqualTo(transaction.getBook().getBookId());
        assertThat(actual.getBook().getTitle()).isEqualTo(transaction.getBook().getTitle());
        assertThat(actual.getBook().getAuthors()).isEqualTo(transaction.getBook().getAuthors());
        assertThat(actual.getBook().getPublicationYear()).isEqualTo(transaction.getBook().getPublicationYear());
    }
    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        bookRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
