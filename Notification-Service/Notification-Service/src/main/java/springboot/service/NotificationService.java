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

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final JavaMailSender mailSender;

    /**
     * Delivery is opt-in so the service is usable without SMTP credentials: notifications
     * are still persisted, but marked PENDING instead of failing every request.
     */
    @Value("${notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

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

    @Transactional(readOnly = true)
    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No notification preference found for user " + userId));
    }

    /**
     * Records the notification, then attempts delivery. The row is persisted whatever the
     * delivery outcome, so a broken mailbox never loses the audit trail.
     */
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

    @Transactional(readOnly = true)
    public List<Notification> getNotificationHistory(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Decides whether to dispatch and records the result on the notification.
     * Never throws: a delivery failure is data, not an error the caller must handle.
     */
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

    private String truncate(String reason) {
        if (reason == null) {
            return "Unknown error";
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }
}
