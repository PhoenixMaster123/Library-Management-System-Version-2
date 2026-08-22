package springboot.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import springboot.model.enums.NotificationStatus;
import springboot.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/** A notification as the API returns it, delivery outcome included. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String subject;
    private String body;
    private String recipientEmail;
    private NotificationType type;
    private NotificationStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
}
