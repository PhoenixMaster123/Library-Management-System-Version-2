package app.adapters.output;

import app.adapters.output.entity.BookEntity;
import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.entity.TransactionEntity;
import app.adapters.output.mapper.EntityMapper;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.domain.model.Book;
import app.domain.model.Transaction;
import app.domain.port.output.TransactionRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persists loans through JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class TransaktionRepositoryPortAdapter implements TransactionRepositoryPort {

    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    /** Stores a loan. Lookups and save share one context, or the cascade onto the book fails. */
    @Override
    public void saveTransaction(Transaction transaction) {
        // The id is left unset: @GeneratedValue assigns it on persist, and setting it here would
        // make save() treat the entity as detached and issue an UPDATE instead of an INSERT.
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setBorrowDate(transaction.getBorrowDate());
        transactionEntity.setReturnDate(transaction.getReturnDate());
        transactionEntity.setDueDate(transaction.getDueDate());

        Optional<CustomerEntity> customerEntity =
                customerRepository.findById(transaction.getCustomer().getCustomerId());
        Optional<BookEntity> bookEntity = bookRepository.findById(transaction.getBook().getBookId());

        if (customerEntity.isEmpty()) {
            throw new EntityNotFoundException("Customer not found");
        }

        if (bookEntity.isEmpty()) {
            throw new EntityNotFoundException("Book not found");
        }

        transactionEntity.setCustomer(customerEntity.get());
        transactionEntity.setBook(bookEntity.get());

        TransactionEntity savedEntity = transactionRepository.save(transactionEntity);

        transaction.setTransactionId(savedEntity.getTransactionId());

    }

    /** Every stored loan recorded against one book. */
    @Override
    public List<Transaction> getTransactionsForBook(Book book) {
        return transactionRepository.findByBookBookId(book.getBookId())
                .stream()
                .map(EntityMapper::toTransaction)
                .toList();
    }

    /** One page of one member's stored loans. */
    @Override
    public Page<Transaction> viewBorrowingHistory(UUID customerID, Pageable pageable) {
        return transactionRepository.findByCustomerCustomerId(customerID, pageable)
                .map(EntityMapper::toTransaction);
    }

    /** The stored loan with this id, or empty. */
    @Override
    public Optional<Transaction> findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(EntityMapper::toTransaction);
    }

    /** The loan a book is out on, or empty when it is on the shelf. */
    @Override
    public Optional<Transaction> findActiveLoanForBook(UUID bookId) {
        return transactionRepository.findFirstByBookBookIdAndReturnDateIsNull(bookId)
                .map(EntityMapper::toTransaction);
    }

    /** One page of every stored loan. */
    @Override
    public Page<Transaction> findAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(EntityMapper::toTransaction);
    }

    /** One page of the loans still outstanding. */
    @Override
    public Page<Transaction> findActiveLoans(Pageable pageable) {
        return transactionRepository.findByReturnDateIsNull(pageable).map(EntityMapper::toTransaction);
    }

    /** How many books a member has out right now. */
    @Override
    public long countActiveLoans(UUID customerId) {
        return transactionRepository.countByCustomerCustomerIdAndReturnDateIsNull(customerId);
    }

    /** Loans still out and falling due on this date. */
    @Override
    public List<Transaction> findLoansDueOn(LocalDate dueDate) {
        return transactionRepository.findByReturnDateIsNullAndDueDate(dueDate)
                .stream()
                .map(EntityMapper::toTransaction)
                .toList();
    }

    /** Writes a changed loan back to storage. */
    @Override
    public void updateTransaction(Transaction transaction) {
        TransactionEntity entity = transactionRepository.findById(transaction.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        entity.setReturnDate(transaction.getReturnDate());
        entity.setDueDate(transaction.getDueDate());
        entity.setExtended(transaction.isExtended());
        transactionRepository.save(entity);
    }
}
