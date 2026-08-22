package springboot.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "subject is required")
    private String subject;

    private String body;

    /**
     * Optional override. When absent, the address from the user's stored preference is used.
     */
    @Email(message = "recipientEmail must be a valid email address")
    private String recipientEmail;
}
