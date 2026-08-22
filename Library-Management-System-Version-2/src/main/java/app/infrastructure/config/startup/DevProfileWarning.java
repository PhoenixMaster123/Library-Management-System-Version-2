package app.infrastructure.config.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Prints a warning at start-up when the dev profile is active. */
@Component
@Profile("dev")
@Slf4j
public class DevProfileWarning {

    /** Logs what the dev profile loosens, once the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void warn() {
        log.warn("""

                ################################################################
                Running with the 'dev' profile:

                  - administrator password is 'admin'
                  - the JWT signing key is the one committed to this repository
                  - the H2 console answers without authentication
                  - Swagger UI publishes the whole API

                Never run this profile anywhere reachable from the internet.
                ################################################################
                """);
    }
}
