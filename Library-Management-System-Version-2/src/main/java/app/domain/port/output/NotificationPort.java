package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.ReminderSetting;

import java.time.LocalDate;
import java.util.UUID;

/** Tells a member something happened to their loan. Implementations swallow delivery failures. */
public interface NotificationPort {

    /** Tells a member what they borrowed and when it is due back. */
    void notifyBookBorrowed(Customer customer, Book book, LocalDate dueDate);

    /** Confirms to a member that a book came back. */
    void notifyBookReturned(Customer customer, Book book);

    /** Sent while the book is still out, a few days before it is due back. */
    void notifyDueSoon(Customer customer, Book book, LocalDate dueDate);

    /** Mirrors a member's reminder choice outward. Best-effort; ReminderPreferencePort is the record. */
    void saveReminderSetting(UUID customerId, ReminderSetting setting);
}
