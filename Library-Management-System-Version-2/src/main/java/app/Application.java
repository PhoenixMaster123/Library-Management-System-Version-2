package app;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Boots the library backend, with Feign clients, scheduling and async enabled. */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
public class Application {
    /** Starts the application. */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
