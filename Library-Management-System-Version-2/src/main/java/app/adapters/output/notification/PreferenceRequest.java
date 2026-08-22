package app.adapters.output.notification;

import java.util.UUID;

/** Wire format of Notification-Service's {@code POST /preferences}. */
public record PreferenceRequest(UUID userId, String contactEmail, boolean notificationEnabled, String type) {

    /** A preference to be delivered by email. */
    public static PreferenceRequest email(UUID userId, String contactEmail, boolean enabled) {
        return new PreferenceRequest(userId, contactEmail, enabled, "EMAIL");
    }
}
