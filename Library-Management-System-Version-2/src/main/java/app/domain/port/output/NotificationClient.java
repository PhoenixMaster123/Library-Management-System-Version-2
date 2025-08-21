package app.domain.port.output;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "notification-service", url = "http://localhost:9093/api/v1/notifications")
public interface NotificationClient {

    //@GetMapping("/sendNotification")
    //void sendNotification(String title, String message);

    @GetMapping("/test")
    ResponseEntity<String> getHelloMessage() {
        return ResponseEntity.ok("Hello from notification service");
    }
}
