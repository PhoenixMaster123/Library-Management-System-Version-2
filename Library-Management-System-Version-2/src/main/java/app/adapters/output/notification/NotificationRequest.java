package app.adapters.output.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Wire format for Notification-Service's send endpoint. Adapter-local: the domain must not know it. */
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
