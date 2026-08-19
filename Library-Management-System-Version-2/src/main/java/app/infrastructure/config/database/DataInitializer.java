package app.infrastructure.config.database;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the bootstrap administrator, the one account self-registration cannot produce.
 *
 * <p>Credentials come from configuration; set {@code LIBRARY_ADMIN_PASSWORD} outside local runs.
 */
@Component
@Slf4j
public class DataInitializer {

    private static final String DEFAULT_PASSWORD = "admin";

    @Value("${library.admin.username:admin}")
    private String adminUsername;

    @Value("${library.admin.password:" + DEFAULT_PASSWORD + "}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initDatabase(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("library.admin.password is blank - no administrator account was created.");
                return;
            }
            if (repository.findByUsername(adminUsername).isPresent()) {
                return;
            }

            repository.save(new UserEntity(adminUsername, passwordEncoder.encode(adminPassword), "ADMIN"));

            if (DEFAULT_PASSWORD.equals(adminPassword)) {
                log.warn("Administrator '{}' created with the default password. "
                        + "Set LIBRARY_ADMIN_PASSWORD before deploying this anywhere.", adminUsername);
            } else {
                log.info("Administrator '{}' created.", adminUsername);
            }
        };
    }
}
