package app.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** One loan: who borrowed what, when it is due, and whether it came back. */
@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    private UUID transactionId;
    private UUID customerId;
    private UUID bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    /** A loan may be extended once; this is what stops a second one. */
    private boolean extended;
    private Customer customer;
    private Book book;

    public Transaction(UUID transactionId, LocalDate borrowDate, LocalDate returnDate,
                       LocalDate dueDate, Customer customer, Book book) {
        this.transactionId = transactionId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.dueDate = dueDate;
        this.setCustomer(customer);
        this.setBook(book);
    }

    public Transaction(LocalDate borrowDate, LocalDate dueDate, Customer customer, Book book) {
        this.transactionId = UUID.randomUUID();
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.setCustomer(customer);
        this.setBook(book);
    }

    public Transaction(UUID transactionId, LocalDate borrowDate, LocalDate returnDate, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.dueDate = dueDate;
    }

    /**
     * Also derives {@link #customerId}. Final because the constructors call it: an overridable
     * method invoked during construction can run subclass code against a half-built object.
     */
    public final void setCustomer(Customer customer) {
        this.customer = customer;
        this.customerId = customer != null ? customer.getCustomerId() : null;
    }

    /** Also derives {@link #bookId}. Final for the same reason as {@link #setCustomer}. */
    public final void setBook(Book book) {
        this.book = book;
        this.bookId = book != null ? book.getBookId() : null;
    }
}
