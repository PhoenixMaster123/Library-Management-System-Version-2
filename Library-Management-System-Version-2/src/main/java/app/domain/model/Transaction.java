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

    /** A loan already known by id, with the member and book attached. */
    public Transaction(UUID transactionId, LocalDate borrowDate, LocalDate returnDate,
                       LocalDate dueDate, Customer customer, Book book) {
        this.transactionId = transactionId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.dueDate = dueDate;
        this.setCustomer(customer);
        this.setBook(book);
    }

    /** A brand-new loan: generates its id and leaves the return date open. */
    public Transaction(LocalDate borrowDate, LocalDate dueDate, Customer customer, Book book) {
        this.transactionId = UUID.randomUUID();
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.setCustomer(customer);
        this.setBook(book);
    }

    /** Just the dates, for callers that attach the member and book afterwards. */
    public Transaction(UUID transactionId, LocalDate borrowDate, LocalDate returnDate, LocalDate dueDate) {
        this.transactionId = transactionId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
        this.dueDate = dueDate;
    }

    /** Sets the borrower and derives customerId. Final because the constructors call it. */
    public final void setCustomer(Customer customer) {
        this.customer = customer;
        this.customerId = customer != null ? customer.getCustomerId() : null;
    }

    /** Sets the book and derives bookId. Final for the same reason as setCustomer. */
    public final void setBook(Book book) {
        this.book = book;
        this.bookId = book != null ? book.getBookId() : null;
    }
}
