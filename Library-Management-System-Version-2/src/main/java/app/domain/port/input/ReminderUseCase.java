package app.domain.port.input;

import app.domain.model.ReminderSetting;

import java.util.UUID;

/** Reading and changing a member's due-date reminder preference. */
public interface ReminderUseCase {

    /** The member's current choice, with the address reminders would go to. */
    ReminderSetting settingFor(UUID customerId);

    /**
     * Turns reminders on or off. The address is never taken from the caller - it is the one on the
     * membership - so switching reminders on cannot redirect them somewhere else.
     */
    ReminderSetting updateSetting(UUID customerId, boolean enabled);
}
