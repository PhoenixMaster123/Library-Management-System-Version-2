package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Customer;

/**
 * Announces that a loan started or ended, for whoever cares to listen - today the analytics
 * service. Like notifications, publishing must never fail a borrow: implementations swallow
 * transport errors.
 */
public interface LoanEventPort {

    void bookBorrowed(Customer customer, Book book);

    void bookReturned(Customer customer, Book book);
}
