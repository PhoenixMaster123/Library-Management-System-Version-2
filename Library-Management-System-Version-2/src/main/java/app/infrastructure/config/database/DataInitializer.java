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

/**
 * Creates the bootstrap administrator, the one account self-registration cannot produce.
 *
 * <p>A configured {@code library.admin.password} is authoritative and is reapplied on every
 * start-up. That matters now the database is file-backed: the account outlives the process, so
 * creating it only when missing would mean setting the variable later had no effect at all.
 *
 * <p>With nothing configured a password is generated and written to the log, so there is always a
 * way in without a well-known one being baked into a public repository.
 */
@Component
@Slf4j
public class DataInitializer {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${library.admin.username:admin}")
    private String adminUsername;

    @Value("${library.admin.password:}")
    private String adminPassword;

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

    /**
     * Brings a stored account back in line with configuration.
     *
     * <p>Only when a password is configured: a generated one must not be reapplied, or it would
     * change on every restart and lock out whoever had just been told it.
     */
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

    private static String generatePassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

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
