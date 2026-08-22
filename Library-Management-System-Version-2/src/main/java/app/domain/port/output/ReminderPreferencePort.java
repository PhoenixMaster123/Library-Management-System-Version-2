package app.domain.port.output;

import java.util.UUID;

/** The authoritative store for a member's due-date reminder choice. Never read back from elsewhere. */
public interface ReminderPreferencePort {

    /** Records a member's reminder choice. */
    void setEnabled(UUID customerId, boolean enabled);

    /** False for a member who has never chosen - reminders are opt-in. */
    boolean isEnabled(UUID customerId);
}
