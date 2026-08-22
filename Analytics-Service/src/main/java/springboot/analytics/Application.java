package springboot.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Boots Analytics-Service. */
@SpringBootApplication
public class Application {

    /** Starts the service. */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
