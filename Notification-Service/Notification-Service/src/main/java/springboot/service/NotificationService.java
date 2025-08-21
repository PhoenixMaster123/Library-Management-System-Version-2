package springboot.service;

import org.springframework.stereotype.Service;
import springboot.model.Notification;
import springboot.model.NotificationPreference;
import springboot.web.dto.NotificationRequest;
import springboot.web.dto.UpsertNotificationPreference;

import java.util.Collection;
import java.util.UUID;

@Service
public class NotificationService {

    public NotificationPreference upsertPreference(UpsertNotificationPreference upsertNotificationPreference) {
        return null;
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return null;
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {
        return null;
    }

    public Collection<Object> getNotificationHistory(UUID userId) {
        return null;
    }
}
