package app.domain.port.output;

import java.util.UUID;

/**
 * Where a member's due-date reminder choice is kept.
 *
 * <p>This is the authoritative store. Notification-Service holds a copy so it can address the
 * member, but it is written to best-effort and never read back: a member who ticks the box while
 * that service is down must still find the box ticked on their next visit.
 */
public interface ReminderPreferencePort {

    void setEnabled(UUID customerId, boolean enabled);

    /** False for a member who has never chosen - reminders are opt-in. */
    boolean isEnabled(UUID customerId);
}
