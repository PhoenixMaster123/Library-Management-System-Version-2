package springboot.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springboot.model.Notification;
import springboot.model.NotificationPreference;
import springboot.service.NotificationService;
import springboot.web.dto.NotificationPreferenceResponse;
import springboot.web.dto.NotificationRequest;
import springboot.web.dto.NotificationResponse;
import springboot.web.dto.UpsertNotificationPreference;
import springboot.web.mapper.DtoMapper;

import java.util.List;
import java.util.UUID;

/** The service's HTTP surface. Unauthenticated: it trusts the userId it is given. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Creates or overwrites the caller's delivery preference; answers 201. */
    @PostMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> upsertNotificationPreference(
            @Valid @RequestBody UpsertNotificationPreference upsertNotificationPreference) {

        NotificationPreference notificationPreference =
                notificationService.upsertPreference(upsertNotificationPreference);

        NotificationPreferenceResponse responseDto =
                DtoMapper.fromNotificationPreference(notificationPreference);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDto);
    }

    /** The stored preference for one user; 404 when there is none. */
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getUserNotificationPreference(
            @RequestParam(name = "userId") UUID userId) {

        NotificationPreference notificationPreference = notificationService.getPreferenceByUserId(userId);

        NotificationPreferenceResponse responseDto =
                DtoMapper.fromNotificationPreference(notificationPreference);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(responseDto);
    }

    /** Raises a notification and reports how delivery went; answers 201. */
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest notificationRequest) {

        Notification notification = notificationService.sendNotification(notificationRequest);

        NotificationResponse response = DtoMapper.fromNotification(notification);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /** Everything raised for one user, newest first. */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotificationHistory(
            @RequestParam(name = "userId") UUID userId) {

        List<NotificationResponse> notificationHistory = notificationService.getNotificationHistory(userId)
                .stream()
                .map(DtoMapper::fromNotification)
                .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(notificationHistory);
    }

    /** Liveness probe kept for manual checks; returns a fixed string. */
    @GetMapping("/test")
    public ResponseEntity<String> getHelloWorld() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Test, Hello World");
    }
}
