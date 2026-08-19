package app.adapters.output.notification;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** The base URL is configurable so the service can move without a recompile. */
@FeignClient(
        name = "notification-service",
        url = "${notification.service.url:http://localhost:9093/api/v1/notifications}"
)
/** HTTP client for Notification-Service. The base URL is configurable so the service can move. */
public interface NotificationFeignClient {

    @PostMapping
    ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request);

    /**
     * Mirrors a reminder choice. There is deliberately no read counterpart: the library keeps the
     * authoritative copy, so this service being down cannot lose a member's preference.
     */
    @PostMapping("/preferences")
    ResponseEntity<String> upsertPreference(@RequestBody PreferenceRequest request);
}
