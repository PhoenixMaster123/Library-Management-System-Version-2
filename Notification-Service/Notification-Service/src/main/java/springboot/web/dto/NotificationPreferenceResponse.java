package springboot.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import springboot.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID userId;
    private String contactEmail;
    private boolean notificationEnabled;
    private NotificationType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
