package app.adapters.output.H2;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.entity.TransactionEntity;
import app.adapters.output.TransaktionRepositoryPortAdapter;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.domain.models.Author;
import app.domain.models.Book;
import app.domain.models.Customer;
import app.domain.models.Transaction;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class TransaktionDaoAdapterTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CustomerRepository customerRepository;

    private TransaktionRepositoryPortAdapter transactionDaoAdapter;

    @BeforeEach
    public void setUp() {
        transactionDaoAdapter = new TransaktionRepositoryPortAdapter(transactionRepository, bookRepository, customerRepository);
    }

    @Test
    public void testAddTransaction_Success(){
        UUID customerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(), null, null, null,
                new Customer(customerId, "John Doe", "john.doe@example.com", true),
                new Book(bookId, "Clean Code", "9780132350884", 2008, true, null, new HashSet<>())
        );

        Optional<CustomerEntity> customerEntity = Optional.of(new CustomerEntity());
        customerEntity.get().setCustomerId(customerId);
        Optional<BookEntity> bookEntity = Optional.of(new BookEntity());
        bookEntity.get().setBookId(bookId);

        Mockito.when(customerRepository.findById(customerId)).thenReturn(customerEntity);
        Mockito.when(bookRepository.findById(bookId)).thenReturn(bookEntity);

        TransactionEntity savedTransactionEntity = new TransactionEntity();
        savedTransactionEntity.setTransactionId(UUID.randomUUID());
        Mockito.when(transactionRepository.save(Mockito.any(TransactionEntity.class))).thenReturn(savedTransactionEntity);

        transactionDaoAdapter.saveTransaction(transaction);

        Mockito.verify(transactionRepository).save(Mockito.any(TransactionEntity.class));

        assertNotNull(transaction.getTransactionId(), "Transaction ID should not be null");

        assertEquals(transaction.getCustomer().getCustomerId(), customerId);
        assertEquals(transaction.getBook().getBookId(), bookId);
    }



    @Test
    public void testAddTransaction_CustomerNotFound(){
        Transaction transaction = new Transaction(
                UUID.randomUUID(), null, null, null,
                new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true),
                new Book(UUID.randomUUID(), "Clean Code", "9780132350884", 2008, true, null, new HashSet<>())
        );

        Mockito.when(customerRepository.findById(transaction.getCustomer().getCustomerId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionDaoAdapter.saveTransaction(transaction), "Expected EntityNotFoundException to be thrown");
    }


    @Test
    public void testAddTransaction_BookNotFound(){
        UUID customerId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(), null, null, null,
                new Customer(customerId, "John Doe", "john.doe@example.com", true),
                new Book(UUID.randomUUID(), "Clean Code", "9780132350884", 2008, true, null, new HashSet<>())
        );

        Optional<CustomerEntity> customerEntity = Optional.of(new CustomerEntity());
        customerEntity.get().setCustomerId(customerId);
        Mockito.when(customerRepository.findById(customerId)).thenReturn(customerEntity);
        Mockito.when(bookRepository.findById(transaction.getBook().getBookId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionDaoAdapter.saveTransaction(transaction), "Expected EntityNotFoundException to be thrown");
    }


    @Test
    public void testGetTransactionsForBook() {
        UUID bookId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        List<TransactionEntity> transactionEntities = new ArrayList<>();

        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setTransactionId(UUID.randomUUID());
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setCustomerId(customerId);
        transactionEntity.setCustomer(customerEntity);

        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(bookId);
        transactionEntity.setBook(bookEntity);

        transactionEntities.add(transactionEntity);

        Mockito.when(transactionRepository.findByBookBookId(bookId)).thenReturn(transactionEntities);

        List<Transaction> transactions = transactionDaoAdapter.getTransactionsForBook(new Book(bookId, "TitelTest", "123124124", 2025, true, LocalDate.now(), new HashSet<>()));

        assertEquals(1, transactions.size());
        assertEquals(customerId, transactions.getFirst().getCustomer().getCustomerId());
        Mockito.verify(transactionRepository).findByBookBookId(bookId);
    }

    @Test
    public void testViewBorrowingHistory() {
        Pageable pageable = Pageable.ofSize(10);

        TransactionEntity transactionEntity1 = new TransactionEntity();
        transactionEntity1.setTransactionId(UUID.randomUUID());
        transactionEntity1.setBorrowDate(LocalDate.now());
        transactionEntity1.setReturnDate(LocalDate.now());
        transactionEntity1.setDueDate(LocalDate.of(2025,1,5));

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setCustomerId(UUID.randomUUID());
        transactionEntity1.setCustomer(customerEntity);

        TransactionEntity transactionEntity2 = new TransactionEntity();
        transactionEntity2.setTransactionId(UUID.randomUUID());
        transactionEntity2.setCustomer(customerEntity);

        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(UUID.randomUUID());
        transactionEntity1.setBook(bookEntity);
        transactionEntity2.setBook(bookEntity);

        Page<TransactionEntity> transactionEntityPage = new PageImpl<>(List.of(transactionEntity1, transactionEntity2), pageable, 2);

        Mockito.when(transactionRepository.findByCustomerCustomerId(customerEntity.getCustomerId(), pageable)).thenReturn(transactionEntityPage);

        Page<Transaction> transactions = transactionDaoAdapter.viewBorrowingHistory(customerEntity.getCustomerId(), pageable);

        assertEquals(2, transactions.getTotalElements());
        Mockito.verify(transactionRepository).findByCustomerCustomerId(customerEntity.getCustomerId(), pageable);
        assertEquals(customerEntity.getCustomerId(), transactions.getContent().get(0).getCustomer().getCustomerId());
        assertEquals(customerEntity.getCustomerId(), transactions.getContent().get(1).getCustomer().getCustomerId());
    }

    @Test
    public void testFindTransactionById_Found() {
        UUID transactionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setCustomerId(customerId);

        BookEntity bookEntity = new BookEntity();
        bookEntity.setBookId(UUID.randomUUID());

        AuthorEntity authorEntity1 = new AuthorEntity();
        authorEntity1.setAuthorId(UUID.randomUUID());
        authorEntity1.setName("Author One");
        authorEntity1.setBio("Bio of Author One");

        AuthorEntity authorEntity2 = new AuthorEntity();
        authorEntity2.setAuthorId(UUID.randomUUID());
        authorEntity2.setName("Author Two");
        authorEntity2.setBio("Bio of Author Two");

        Set<AuthorEntity> authors = new HashSet<>();
        authors.add(authorEntity1);
        authors.add(authorEntity2);
        bookEntity.setAuthors(authors);

        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setTransactionId(transactionId);
        transactionEntity.setCustomer(customerEntity);
        transactionEntity.setBook(bookEntity);

        Mockito.when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionEntity));

        Optional<Transaction> transaction = transactionDaoAdapter.findTransactionById(transactionId);

        assertTrue(transaction.isPresent());
        assertEquals(transactionId, transaction.get().getTransactionId());
        assertEquals(customerId, transaction.get().getCustomer().getCustomerId());  // Check that the customer ID is correctly mapped

        Set<Author> mappedAuthors = transaction.get().getBook().getAuthors();
        assertNotNull(mappedAuthors);
        assertEquals(2, mappedAuthors.size());
        assertTrue(mappedAuthors.stream().anyMatch(author -> author.getAuthorId().equals(authorEntity1.getAuthorId())));
        assertTrue(mappedAuthors.stream().anyMatch(author -> author.getAuthorId().equals(authorEntity2.getAuthorId())));

        Mockito.verify(transactionRepository).findById(transactionId);
    }




    @Test
    public void testFindTransactionById_NotFound() {
        UUID transactionId = UUID.randomUUID();

        Mockito.when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        Optional<Transaction> transaction = transactionDaoAdapter.findTransactionById(transactionId);

        assertFalse(transaction.isPresent());
        Mockito.verify(transactionRepository).findById(transactionId);
    }

    @Test
    public void testUpdateTransaction_Success() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(transactionId, null, LocalDate.now(), LocalDate.now().plusDays(14));
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setTransactionId(transactionId);

        Mockito.when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionEntity));

        transactionDaoAdapter.updateTransaction(transaction);

        Mockito.verify(transactionRepository).findById(transactionId);
        Mockito.verify(transactionRepository).save(transactionEntity);
        assertEquals(transaction.getReturnDate(), transactionEntity.getReturnDate());
        assertEquals(transaction.getDueDate(), transactionEntity.getDueDate());
    }

    @Test
    public void testUpdateTransaction_NotFound() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(transactionId, null, LocalDate.now(), LocalDate.now().plusDays(14));

        Mockito.when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionDaoAdapter.updateTransaction(transaction), "Expected EntityNotFoundException to be thrown");
    }

}