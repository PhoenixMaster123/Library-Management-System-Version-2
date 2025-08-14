package app.domain.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class Transaction {
    private UUID transactionId;
    private UUID customerId;
    private UUID bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Customer customer;
    private Book book;
    public Transaction(UUID transactionId, LocalDate borrowDate, LocalDate returnDate, LocalDate dueDate, Customer customer, Book book) {
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
    public Transaction() {

    }
    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.customerId = customer != null ? customer.getCustomerId() : null;
    }

    public void setBook(Book book) {
        this.book = book;
        this.bookId = book != null ? book.getBookId() : null;
    }
}
