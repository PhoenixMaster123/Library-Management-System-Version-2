package app.adapters.output.notification;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.ReminderSetting;
import app.domain.port.output.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Calls Notification-Service. Every failure is swallowed and logged, so it cannot fail a borrow. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceAdapter implements NotificationPort {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final NotificationFeignClient client;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /** Emails the member what they borrowed and when it is due. */
    @Override
    public void notifyBookBorrowed(Customer customer, Book book, LocalDate dueDate) {
        String subject = "You borrowed \"" + book.getTitle() + "\"";
        String body = "Hi " + customer.getName() + ",\n\n"
                + "You borrowed \"" + book.getTitle() + "\" (ISBN " + book.getIsbn() + ").\n"
                + "Please return it by " + dueDate.format(DATE) + ".\n\n"
                + "Happy reading!\nThe Library";

        dispatch(customer, subject, body);
    }

    /** Emails the member that their return was received. */
    @Override
    public void notifyBookReturned(Customer customer, Book book) {
        String subject = "Thanks for returning \"" + book.getTitle() + "\"";
        String body = "Hi " + customer.getName() + ",\n\n"
                + "We received your return of \"" + book.getTitle() + "\" (ISBN " + book.getIsbn() + ").\n"
                + "Your loan is now closed.\n\n"
                + "The Library";

        dispatch(customer, subject, body);
    }

    /** Emails the member that a book is due back soon. */
    @Override
    public void notifyDueSoon(Customer customer, Book book, LocalDate dueDate) {
        String subject = "\"" + book.getTitle() + "\" is due back soon";
        String body = "Hi " + customer.getName() + ",\n\n"
                + "\"" + book.getTitle() + "\" is due back on " + dueDate.format(DATE) + ".\n"
                + "You can return it or extend the loan once from your account.\n\n"
                + "The Library";

        dispatch(customer, subject, body);
    }

    /** Mirrors a reminder choice outward, swallowing any failure. */
    @Override
    public void saveReminderSetting(UUID customerId, ReminderSetting setting) {
        try {
            client.upsertPreference(PreferenceRequest.email(customerId, setting.email(), setting.enabled()));
        } catch (Exception e) {
            log.warn("Could not store reminder setting for customer {}: {}", customerId, e.getMessage());
        }
    }

    /** Sends one message, unless notifications are off or the member has no id. Never throws. */
    private void dispatch(Customer customer, String subject, String body) {
        if (!notificationsEnabled) {
            return;
        }
        if (customer == null || customer.getCustomerId() == null) {
            log.warn("Cannot send notification: customer or customer id is missing");
            return;
        }

        try {
            client.sendNotification(new NotificationRequest(
                    customer.getCustomerId(),
                    subject,
                    body,
                    customer.getEmail()
            ));
            log.info("Notification queued for customer {} ({})", customer.getName(), customer.getEmail());
        } catch (Exception e) {
            // Intentionally not rethrown - see NotificationPort's contract.
            log.warn("Notification for customer {} could not be delivered: {}",
                    customer.getCustomerId(), e.getMessage());
        }
    }
}
