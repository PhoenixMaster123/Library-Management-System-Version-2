package app.domain.services.unitTests;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.TransactionRepositoryPort;
import app.domain.dto.CreateNewTransaktion;
import app.domain.services.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")
class TransactionUseCaseTest {
        @Mock
        private TransactionRepositoryPort transactionRepositoryPort;
        @Mock
        private BookRepositoryPort bookRepositoryPort;
        @Mock
        private CustomerRepositoryPort customerRepositoryPort;
        @InjectMocks
        private TransactionService transactionService;

        @BeforeEach
        void setUp() {
            transactionService = new TransactionService(transactionRepositoryPort, bookRepositoryPort, customerRepositoryPort);
        }

        @Test
        void testCreateNewTransaction_ValidInput_Success() {
            CreateNewTransaktion createNewTransaktion = new CreateNewTransaktion(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(10),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );

            Customer expectedCustomer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
            Book expectedBook = new Book(UUID.randomUUID(), "Clean Code", "Robert C. Martin", 2008, true, null);

            when(customerRepositoryPort.getCustomer(createNewTransaktion.getCustomerId())).thenReturn(Optional.of(expectedCustomer));
            when(bookRepositoryPort.searchBookById(createNewTransaktion.getBookId())).thenReturn(Optional.of(expectedBook));

            Transaction transaction = transactionService.createNewTransaction(createNewTransaktion);

            verify(transactionRepositoryPort).saveTransaction(transaction);
            assertThat(transaction.getCustomer()).isEqualTo(expectedCustomer);
            assertThat(transaction.getBook()).isEqualTo(expectedBook);
        }

        @Test
        void testCreateNewTransaction_BorrowDateAfterDueDate_ThrowsException() {
            CreateNewTransaktion createNewTransaktion = new CreateNewTransaktion(
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(5),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );

            assertThrows(IllegalArgumentException.class, () -> transactionService.createNewTransaction(createNewTransaktion));
            verifyNoInteractions(transactionRepositoryPort);
        }

        @Test
        void testCreateNewTransaction_DueDateBeforeToday_ThrowsException() {
            CreateNewTransaktion createNewTransaktion = new CreateNewTransaktion(
                    LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(1),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );

            assertThrows(IllegalArgumentException.class, () -> transactionService.createNewTransaction(createNewTransaktion));
            verifyNoInteractions(transactionRepositoryPort);
        }

        @Test
        void testCreateNewTransaction_CustomerNotFound_ThrowsException() {
            CreateNewTransaktion createNewTransaktion = new CreateNewTransaktion(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(10),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );
            when(customerRepositoryPort.getCustomer(createNewTransaktion.getCustomerId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.createNewTransaction(createNewTransaktion));
            verifyNoInteractions(transactionRepositoryPort);
        }

        @Test
        void testCreateNewTransaction_BookNotFound_ThrowsException() {
            CreateNewTransaktion createNewTransaktion = new CreateNewTransaktion(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(10),
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );
            when(customerRepositoryPort.getCustomer(createNewTransaktion.getCustomerId())).thenReturn(Optional.of(new Customer()));
            when(bookRepositoryPort.searchBookById(createNewTransaktion.getBookId())).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> transactionService.createNewTransaction(createNewTransaktion));
            verifyNoInteractions(transactionRepositoryPort);
        }

        @Test
        void testReturnBook_NoTransactionFound_ThrowsException() {
            Book book = new Book(UUID.randomUUID(), null, null, 0, false, null);

            when(transactionRepositoryPort.getTransactionsForBook(argThat(b ->
                    b.getBookId().equals(book.getBookId())))).thenReturn(Collections.emptyList());

            assertThrows(EntityNotFoundException.class, () -> transactionService.returnBook(book.getBookId()));

            verifyNoInteractions(bookRepositoryPort);
        }


        @Test
        void testReturnBook_Success() {
            UUID bookId = UUID.randomUUID();
            Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, false, null); // Book is already borrowed
            Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
            when(transactionRepositoryPort.getTransactionsForBook(any(Book.class))).thenReturn(List.of(transaction));

            String transactionId = transactionService.returnBook(bookId);

            assertThat(transactionId).isEqualTo(transaction.getTransactionId().toString());
            assertThat(transaction.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(transaction.getBook().isAvailable()).isTrue();
            verify(transactionRepositoryPort).updateTransaction(transaction);
            verify(bookRepositoryPort).updateBook(bookId, transaction.getBook());
        }

        @Test
        void testBorrowBook_BookNotFound_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> transactionService.borrowBook(customerId, bookId));

            verifyNoInteractions(customerRepositoryPort, transactionRepositoryPort);

            verify(bookRepositoryPort).searchBookById(bookId);
        }


        @Test
        void testBorrowBook_CustomerNotFound_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, true, null);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> transactionService.borrowBook(customerId, bookId));
            verify(bookRepositoryPort).searchBookById(bookId);
        }

        @Test
        void testBorrowBook_BookNotAvailable_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, false, null); // Book is already borrowed
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book)); // Only stub what's relevant

            assertThrows(RuntimeException.class, () -> transactionService.borrowBook(customerId, bookId));

            verify(bookRepositoryPort).searchBookById(bookId);
        }


        @Test
        void testBorrowBook_CustomerNoPrivileges_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, true, null);
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", false);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(customer));

            assertThrows(RuntimeException.class, () -> transactionService.borrowBook(customerId, bookId));
            verify(bookRepositoryPort).searchBookById(bookId);
        }

        @Test
        void testBorrowBook_Success() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, true, null);
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(customer));

            Transaction transaction = transactionService.borrowBook(customerId, bookId);

            assertThat(transaction.getCustomer()).isEqualTo(customer);
            assertThat(transaction.getBook()).isEqualTo(book);
            assertThat(transaction.getBorrowDate()).isBetween(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
            assertThat(transaction.getDueDate()).isBetween(LocalDate.now().plusWeeks(2).minusDays(1), LocalDate.now().plusWeeks(2).plusDays(1));

            verify(bookRepositoryPort).searchBookById(bookId);
            verify(customerRepositoryPort).getCustomer(customerId);
            verify(transactionRepositoryPort).saveTransaction(transaction);

            assertThat(book.isAvailable()).isFalse();
        }

        @Test
        void testViewBorrowingHistory() {
            UUID customerId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> expectedPage = mock(Page.class);
            when(transactionRepositoryPort.viewBorrowingHistory(customerId, pageable)).thenReturn(expectedPage);

            Page<Transaction> actualPage = transactionService.viewBorrowingHistory(customerId, pageable);

            assertThat(actualPage).isEqualTo(expectedPage);
            verify(transactionRepositoryPort).viewBorrowingHistory(customerId, pageable);
        }

        @Test
        void testFindById() {
            Transaction transaction = new Transaction(LocalDate.now(), LocalDate.now().plusDays(10), new Customer(), new Book());
            when(transactionRepositoryPort.findTransactionById(transaction.getTransactionId())).thenReturn(Optional.of(transaction));

            Optional<Transaction> actualTransaction = transactionService.findById(transaction.getTransactionId());

            assertThat(actualTransaction).isPresent();
            assertThat(actualTransaction.get()).isEqualTo(transaction);
            verify(transactionRepositoryPort).findTransactionById(transaction.getTransactionId());
        }

        @Test
        void testFindById_NotFound() {
            UUID transactionId = UUID.randomUUID();
            when(transactionRepositoryPort.findTransactionById(transactionId)).thenReturn(Optional.empty());

            Optional<Transaction> actualTransaction = transactionService.findById(transactionId);

            assertThat(actualTransaction).isEmpty();
            verify(transactionRepositoryPort).findTransactionById(transactionId);
        }

        @Test
        void testBorrowBookWithDates_BookNotFound_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            LocalDate borrowDate = LocalDate.now().plusDays(1);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.empty()); // Stub bookDao to return empty

            assertThrows(IllegalStateException.class, () -> transactionService.borrowBookWithDates(customerId, bookId, borrowDate));

            verifyNoInteractions(customerRepositoryPort, transactionRepositoryPort);

            verify(bookRepositoryPort).searchBookById(bookId);
        }


        @Test
        void testBorrowBookWithDates_CustomerNotFound_ThrowsException() {
            // Given
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            LocalDate borrowDate = LocalDate.now().plusDays(1);
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, true, null);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> transactionService.borrowBookWithDates(customerId, bookId, borrowDate));
            verify(bookRepositoryPort).searchBookById(bookId);
        }

        @Test
        void testBorrowBookWithDates_BookNotAvailable_ThrowsException() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            LocalDate borrowDate = LocalDate.now().plusDays(1);
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, false, null);
            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));

            assertThrows(IllegalArgumentException.class, () -> transactionService.borrowBookWithDates(customerId, bookId, borrowDate));
            verify(bookRepositoryPort).searchBookById(bookId);
        }

        @Test
        void testBorrowBookWithDates_Success() {
            UUID customerId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();
            LocalDate borrowDate = LocalDate.now().plusDays(1);

            Book book = mock(Book.class);
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);

            when(bookRepositoryPort.searchBookById(bookId)).thenReturn(Optional.of(book));
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(customer));
            when(book.isAvailable()).thenReturn(true);

            transactionService.borrowBookWithDates(customerId, bookId, borrowDate);

            verify(bookRepositoryPort).searchBookById(bookId);
            verify(customerRepositoryPort).getCustomer(customerId);

            verify(book).setAvailable(false);

            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepositoryPort).saveTransaction(transactionCaptor.capture());
            Transaction capturedTransaction = transactionCaptor.getValue();
            assertThat(capturedTransaction.getBorrowDate()).isEqualTo(borrowDate);
            assertThat(capturedTransaction.getDueDate()).isEqualTo(borrowDate.plusWeeks(2));
            assertThat(capturedTransaction.getCustomer()).isEqualTo(customer);
            assertThat(capturedTransaction.getBook()).isEqualTo(book);
        }


        @Test
        void testReturnBookWithDates_NoTransactionFound_ThrowsException() {
            UUID bookId = UUID.randomUUID();
            LocalDate returnDate = LocalDate.now();
            when(transactionRepositoryPort.getTransactionsForBook(any(Book.class))).thenReturn(Collections.emptyList());

            assertThrows(EntityNotFoundException.class, () -> transactionService.returnBookWithDates(bookId, returnDate));
            verifyNoInteractions(bookRepositoryPort);
        }

        @Test
        void testReturnBookWithDates_Success() {
            UUID bookId = UUID.randomUUID();
            LocalDate returnDate = LocalDate.now();
            Customer customer = new Customer(UUID.randomUUID(), "John Doe", "john.doe@example.com", true);
            Book book = new Book(bookId, "Clean Code", "Robert C. Martin", 2008, false, null);
            Transaction transaction = new Transaction(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), customer, book);
            when(transactionRepositoryPort.getTransactionsForBook(any(Book.class))).thenReturn(List.of(transaction));

            transactionService.returnBookWithDates(bookId, returnDate);

            assertThat(transaction.getReturnDate()).isEqualTo(returnDate);
            assertThat(transaction.getBook().isAvailable()).isTrue();
            verify(transactionRepositoryPort).updateTransaction(transaction);
            verify(bookRepositoryPort).updateBook(bookId, transaction.getBook());
        }
    }