package springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springboot.model.Notification;
import springboot.model.NotificationPreference;
import springboot.model.enums.NotificationStatus;
import springboot.model.enums.NotificationType;
import springboot.repository.NotificationPreferenceRepository;
import springboot.repository.NotificationRepository;
import springboot.web.dto.NotificationRequest;
import springboot.web.dto.UpsertNotificationPreference;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/** Records notifications, delivers them by mail when enabled, and keeps the per-user settings. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final JavaMailSender mailSender;

    /** Opt-in, so the service runs without SMTP credentials: sends become PENDING rather than failures. */
    @Value("${notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /** Creates or overwrites a user's delivery preference; defaults the channel to EMAIL. */
    @Transactional
    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {
        NotificationPreference preference = preferenceRepository.findByUserId(dto.getUserId())
                .orElseGet(NotificationPreference::new);

        preference.setUserId(dto.getUserId());
        preference.setContactEmail(dto.getContactEmail());
        preference.setNotificationEnabled(dto.isNotificationEnabled());
        preference.setType(dto.getType() != null ? dto.getType() : NotificationType.EMAIL);

        return preferenceRepository.save(preference);
    }

    /** The user's preference, or NoSuchElementException when they have none. */
    @Transactional(readOnly = true)
    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No notification preference found for user " + userId));
    }

    /** Records the notification, then tries to deliver it. The row is saved whatever the outcome. */
    @Transactional
    public Notification sendNotification(NotificationRequest request) {
        Optional<NotificationPreference> preference = preferenceRepository.findByUserId(request.getUserId());

        String recipient = request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank()
                ? request.getRecipientEmail()
                : preference.map(NotificationPreference::getContactEmail).orElse(null);

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setSubject(request.getSubject());
        notification.setBody(request.getBody());
        notification.setRecipientEmail(recipient);
        notification.setType(preference.map(NotificationPreference::getType).orElse(NotificationType.EMAIL));

        applyDelivery(notification, preference.orElse(null));

        return notificationRepository.save(notification);
    }

    /** Everything raised for a user, newest first. */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationHistory(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Decides whether to send and writes the outcome onto the notification. Never throws. */
    private void applyDelivery(Notification notification, NotificationPreference preference) {
        if (preference != null && !preference.isNotificationEnabled()) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setFailureReason("User has notifications disabled");
            return;
        }
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("No recipient address: none supplied and no preference stored");
            return;
        }
        if (!mailEnabled) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setFailureReason("Mail delivery disabled (set notification.mail.enabled=true)");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody() != null ? notification.getBody() : "");

            mailSender.send(message);

            notification.setStatus(NotificationStatus.SUCCEEDED);
            notification.setFailureReason(null);
        } catch (Exception e) {
            log.warn("Failed to deliver notification to {}: {}",
                    notification.getRecipientEmail(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(truncate(e.getMessage()));
        }
    }

    /** Clips a failure reason to the 1000 characters the column holds. */
    private String truncate(String reason) {
        if (reason == null) {
            return "Unknown error";
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }
}
