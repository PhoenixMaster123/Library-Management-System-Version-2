package app.adapters.output.mapper;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.entity.TransactionEntity;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** Turns JPA entities into domain models. Associations stop one level deep to avoid recursion. */
@UtilityClass
public class EntityMapper {

    /** Author with its books; those books carry no authors. */
    public static Author toAuthor(AuthorEntity entity) {
        return new Author(
                entity.getAuthorId(),
                entity.getName(),
                entity.getBio(),
                mapSet(entity.getBooks(), EntityMapper::toBookSummary));
    }

    /** Author on its own, for use inside a book. */
    public static Author toAuthorSummary(AuthorEntity entity) {
        return new Author(entity.getAuthorId(), entity.getName(), entity.getBio());
    }

    /** Book with its authors; those authors carry no books. */
    public static Book toBook(BookEntity entity) {
        Book book = new Book(
                entity.getBookId(),
                entity.getTitle(),
                entity.getIsbn(),
                entity.getPublicationYear(),
                entity.isAvailability(),
                entity.getCreatedAt(),
                mapSet(entity.getAuthors(), EntityMapper::toAuthorSummary));
        book.setDescription(entity.getDescription());
        return book;
    }

    /** Book on its own, for use inside an author. */
    public static Book toBookSummary(BookEntity entity) {
        Book book = new Book(
                entity.getBookId(),
                entity.getTitle(),
                entity.getIsbn(),
                entity.getPublicationYear(),
                entity.isAvailability(),
                entity.getCreatedAt(),
                new HashSet<>());
        book.setDescription(entity.getDescription());
        return book;
    }

    /** Customer with its borrowing history, each entry holding only the ids it links. */
    public static Customer toCustomer(CustomerEntity entity) {
        Customer customer = toCustomerSummary(entity);
        if (entity.getTransactions() != null) {
            entity.getTransactions().forEach(transactionEntity -> {
                Transaction transaction = new Transaction(
                        transactionEntity.getTransactionId(),
                        transactionEntity.getBorrowDate(),
                        transactionEntity.getReturnDate(),
                        transactionEntity.getDueDate());
                transaction.setExtended(transactionEntity.isExtended());
                transaction.setCustomerId(entity.getCustomerId());
                transaction.setBookId(transactionEntity.getBook() == null
                        ? null
                        : transactionEntity.getBook().getBookId());
                customer.getTransactions().add(transaction);
            });
        }
        return customer;
    }

    /** Customer on its own, for use inside a transaction. */
    public static Customer toCustomerSummary(CustomerEntity entity) {
        return new Customer(
                entity.getCustomerId(),
                entity.getName(),
                entity.getEmail(),
                entity.isPrivileges());
    }

    /** Loan with its member and book, each mapped without their own histories. */
    public static Transaction toTransaction(TransactionEntity entity) {
        Transaction transaction = new Transaction(
                entity.getTransactionId(),
                entity.getBorrowDate(),
                entity.getReturnDate(),
                entity.getDueDate(),
                toCustomerSummary(entity.getCustomer()),
                toBook(entity.getBook()));
        transaction.setExtended(entity.isExtended());
        return transaction;
    }

    /** Maps a set of entities through the given mapper. */
    private static <E, D> Set<D> mapSet(Set<E> entities, Function<E, D> mapper) {
        if (entities == null) {
            return new HashSet<>();
        }
        Set<D> mapped = new HashSet<>();
        entities.forEach(entity -> mapped.add(mapper.apply(entity)));
        return mapped;
    }
}
