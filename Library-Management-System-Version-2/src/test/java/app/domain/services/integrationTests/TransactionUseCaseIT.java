package app.domain.services.integrationTests;

import app.domain.dto.CreateNewTransaktion;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.TransactionRepositoryPort;
import app.domain.port.input.TransactionUseCase;
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
public class TransactionUseCaseIT {

    @Autowired
    private TransactionUseCase transactionUseCase;
    @Autowired
    private TransactionRepositoryPort transactionRepositoryPort;
    @Autowired
    private BookRepositoryPort bookRepositoryPort;
    @Autowired
    private CustomerRepositoryPort customerRepositoryPort;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testCreateNewTransaction_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerRepositoryPort.saveCustomer(customer);

        customer = customerRepositoryPort.getCustomer(customer.getCustomerId()).get();

        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookRepositoryPort.saveBook(book);

        CreateNewTransaktion createNewTransaction = new CreateNewTransaktion(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10),
                customer.getCustomerId(),
                book.getBookId()
        );

        Transaction transaction = transactionUseCase.createNewTransaction(createNewTransaction);

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
        customerRepositoryPort.saveCustomer(customer);
        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, false, LocalDate.now());
        bookRepositoryPort.saveBook(book);
        Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
        transactionRepositoryPort.saveTransaction(transaction);

        String transactionId = transactionUseCase.returnBook(book.getBookId());

        Optional<Transaction> returnedTransaction = transactionRepositoryPort.findTransactionById(UUID.fromString(transactionId));
        assertThat(returnedTransaction).isPresent();
        assertThat(returnedTransaction.get().getReturnDate()).isNotNull();
        assertThat(bookRepositoryPort.searchBookById(book.getBookId()).get().isAvailable()).isTrue();
    }

    @Test
    void testBorrowBook_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerRepositoryPort.saveCustomer(customer);

        Book book = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookRepositoryPort.saveBook(book);

        Transaction transaction = transactionUseCase.borrowBook(customer.getCustomerId(), book.getBookId());

        assertThat(transaction.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(transaction.getBook().getBookId()).isEqualTo(book.getBookId());
        assertThat(bookRepositoryPort.searchBookById(book.getBookId()).get().isAvailable()).isFalse();
    }

    @Test
    void testViewBorrowingHistory_Integration() {
        Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
        customerRepositoryPort.saveCustomer(customer);
        Book book1 = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, LocalDate.now());
        bookRepositoryPort.saveBook(book1);
        Book book2 = new Book(UUID.randomUUID(), "Design Patterns", "Gang of Four", 1994, true, LocalDate.now());
        bookRepositoryPort.saveBook(book2);

        transactionUseCase.borrowBook(customer.getCustomerId(), book1.getBookId());
        transactionUseCase.borrowBook(customer.getCustomerId(), book2.getBookId());

        Pageable pageable = PageRequest.of(0, 10);

        Page<Transaction> actualPage = transactionUseCase.viewBorrowingHistory(customer.getCustomerId(), pageable);

        assertThat(actualPage.getContent()).hasSize(2);
    }

    @Test
    void testFindById_Integration() {
        Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
        customerRepositoryPort.saveCustomer(customer);
        Book book = new Book(null, "Clean Code", "Robert C. Martin", 2008, false, LocalDate.now());
        bookRepositoryPort.saveBook(book);
        Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
        transactionRepositoryPort.saveTransaction(transaction);

        Optional<Transaction> actualTransaction = transactionUseCase.findById(transaction.getTransactionId());

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
