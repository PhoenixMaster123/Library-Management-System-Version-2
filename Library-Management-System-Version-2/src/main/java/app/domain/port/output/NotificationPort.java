package app.domain.port.output;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.ReminderSetting;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tells a customer something happened to their loan. Implementations must swallow delivery
 * failures: notifying is a side effect of borrowing, never a precondition of it.
 */
public interface NotificationPort {

    void notifyBookBorrowed(Customer customer, Book book, LocalDate dueDate);

    void notifyBookReturned(Customer customer, Book book);

    /** Sent while the book is still out, a few days before it is due back. */
    void notifyDueSoon(Customer customer, Book book, LocalDate dueDate);

    /**
     * Mirrors a member's reminder choice so the notification service can address them. Best-effort
     * like the rest of this port: {@link ReminderPreferencePort} holds the authoritative copy, so a
     * failure here loses nothing.
     */
    void saveReminderSetting(UUID customerId, ReminderSetting setting);
}
