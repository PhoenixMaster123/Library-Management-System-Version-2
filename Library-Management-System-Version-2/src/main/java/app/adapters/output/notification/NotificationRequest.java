package app.adapters.output.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Wire format expected by Notification-Service's {@code POST /api/v1/notifications}.
 * Adapter-local on purpose: the domain must not know this shape.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private UUID userId;
    private String subject;
    private String body;
    private String recipientEmail;
}
