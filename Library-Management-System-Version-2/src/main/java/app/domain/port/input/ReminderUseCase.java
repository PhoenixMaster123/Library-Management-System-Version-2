package app.domain.port.input;

import app.domain.model.ReminderSetting;

import java.util.UUID;

/** Reading and changing a member's due-date reminder preference. */
public interface ReminderUseCase {

    /** The member's current choice, with the address reminders would go to. */
    ReminderSetting settingFor(UUID customerId);

    /** Turns reminders on or off. The address always comes from the membership, never the caller. */
    ReminderSetting updateSetting(UUID customerId, boolean enabled);
}
