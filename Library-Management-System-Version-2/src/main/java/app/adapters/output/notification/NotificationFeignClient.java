package app.adapters.output.notification;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** HTTP client for Notification-Service. The base URL is configurable so the service can move. */
@FeignClient(
        name = "notification-service",
        url = "${notification.service.url:http://localhost:9093/api/v1/notifications}"
)
public interface NotificationFeignClient {

    /** Posts one notification to be recorded and delivered. */
    @PostMapping
    ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request);

    /** Mirrors a reminder choice. No read counterpart: the library keeps the authoritative copy. */
    @PostMapping("/preferences")
    ResponseEntity<String> upsertPreference(@RequestBody PreferenceRequest request);
}
