package springboot.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import springboot.model.enums.NotificationType;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpsertNotificationPreference {

    @NotNull(message = "userId is required")
    private UUID userId;

    @Email(message = "contactEmail must be a valid email address")
    private String contactEmail;

    private boolean notificationEnabled;

    /** Defaults to EMAIL when omitted. */
    private NotificationType type;
}
