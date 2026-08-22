package app.infrastructure.config.database;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/** Creates the bootstrap administrator. A configured password wins and is reapplied every start-up. */
@Component
@Slf4j
public class DataInitializer {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${library.admin.username:admin}")
    private String adminUsername;

    @Value("${library.admin.password:}")
    private String adminPassword;

    /** Creates the administrator at start-up, or brings the stored one back in line. */
    @Bean
    public CommandLineRunner initDatabase(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            Optional<UserEntity> existing = repository.findByUsername(adminUsername);
            boolean configured = adminPassword != null && !adminPassword.isBlank();

            if (existing.isPresent()) {
                reconcile(existing.get(), configured, repository, passwordEncoder);
                return;
            }

            String password = configured ? adminPassword : generatePassword();
            repository.save(new UserEntity(adminUsername, passwordEncoder.encode(password), "ADMIN"));

            if (configured) {
                log.info("Administrator '{}' created from configuration.", adminUsername);
            } else {
                announceGenerated(password);
            }
        };
    }

    /** Reapplies a configured password to the stored account. Generated ones are left alone. */
    private void reconcile(UserEntity admin, boolean configured,
                           UserRepository repository, PasswordEncoder passwordEncoder) {
        if (!configured) {
            log.info("Administrator '{}' already exists. Set LIBRARY_ADMIN_PASSWORD to choose its "
                    + "password; the stored one is left alone.", adminUsername);
            return;
        }

        if (passwordEncoder.matches(adminPassword, admin.getPassword())) {
            return;
        }

        admin.setPassword(passwordEncoder.encode(adminPassword));
        repository.save(admin);
        log.info("Administrator '{}' password reset to the configured one.", adminUsername);
    }

    /** A random password, used when none is configured. */
    private static String generatePassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Writes a generated password to the log, since it exists nowhere else. */
    private void announceGenerated(String password) {
        // Deliberately loud, and the only place this is ever readable: the alternative is either a
        // password everyone knows or no way to sign in at all.
        log.warn("""

                ----------------------------------------------------------------
                No library.admin.password was set, so one has been generated:

                    username: {}
                    password: {}

                It is only shown here. Set LIBRARY_ADMIN_PASSWORD to choose one.
                ----------------------------------------------------------------
                """, adminUsername, password);
    }
}
