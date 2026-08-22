package springboot.web.mapper;

import lombok.experimental.UtilityClass;
import springboot.model.Notification;
import springboot.model.NotificationPreference;
import springboot.web.dto.NotificationPreferenceResponse;
import springboot.web.dto.NotificationResponse;

/** Turns notification entities into the responses the API returns. */
@UtilityClass
public class DtoMapper {

    public static NotificationPreferenceResponse fromNotificationPreference(
            NotificationPreference notificationPreference) {
        if (notificationPreference == null) {
            return null;
        }
        return NotificationPreferenceResponse.builder()
                .id(notificationPreference.getId())
                .userId(notificationPreference.getUserId())
                .contactEmail(notificationPreference.getContactEmail())
                .notificationEnabled(notificationPreference.isNotificationEnabled())
                .type(notificationPreference.getType())
                .createdAt(notificationPreference.getCreatedAt())
                .updatedAt(notificationPreference.getUpdatedAt())
                .build();
    }

    public static NotificationResponse fromNotification(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .recipientEmail(notification.getRecipientEmail())
                .type(notification.getType())
                .status(notification.getStatus())
                .failureReason(notification.getFailureReason())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
