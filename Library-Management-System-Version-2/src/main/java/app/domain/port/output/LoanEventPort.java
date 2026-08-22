package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Customer;

/** Announces that a loan started or ended. Implementations swallow errors: this never fails a borrow. */
public interface LoanEventPort {

    /** Announces that a member took a book out. */
    void bookBorrowed(Customer customer, Book book);

    /** Announces that a book came back. */
    void bookReturned(Customer customer, Book book);
}
