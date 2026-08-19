package app.domain.model;

/** Whether a member wants due-date reminders, and where to send them. */
public record ReminderSetting(boolean enabled, String email) {
}
