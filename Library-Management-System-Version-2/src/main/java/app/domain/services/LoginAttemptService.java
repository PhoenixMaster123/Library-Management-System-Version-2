package app.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed sign-ins and locks an account out for a while once there have been too many.
 *
 * <p>Without this a password can be guessed at network speed, which makes the strength of the
 * password the only thing standing in the way. Attempts are counted per username rather than per
 * address: an attacker can change address far more easily than they can change whose account they
 * are trying to open.
 *
 * <p>Held in memory, so the count resets when the application does. That is a deliberate limit
 * rather than an oversight - surviving a restart means a shared store, and the point here is to
 * turn an instant guessing loop into a slow one.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private record Attempts(int count, Instant firstFailure, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Value("${library.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${library.login.lockout:PT15M}")
    private Duration lockout;

    /** How long attempts are remembered for; a slow trickle of failures should not add up forever. */
    @Value("${library.login.window:PT15M}")
    private Duration window;

    public boolean isLockedOut(String username) {
        Attempts current = attempts.get(key(username));
        if (current == null || current.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(current.lockedUntil())) {
            attempts.remove(key(username));
            return false;
        }
        return true;
    }

    /** Seconds left on the lockout, for telling the caller when to come back. */
    public long secondsRemaining(String username) {
        Attempts current = attempts.get(key(username));
        if (current == null || current.lockedUntil() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), current.lockedUntil()).toSeconds());
    }

    public void recordFailure(String username) {
        String key = key(username);
        Instant now = Instant.now();

        attempts.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.firstFailure().plus(window))) {
                return new Attempts(1, now, null);
            }

            int count = current.count() + 1;
            if (count >= maxAttempts) {
                log.warn("Sign-in locked for '{}' after {} failed attempts.", username, count);
                return new Attempts(count, current.firstFailure(), now.plus(lockout));
            }
            return new Attempts(count, current.firstFailure(), null);
        });
    }

    /** A success clears the slate, so a typo before a correct password costs nothing. */
    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(java.util.Locale.ROOT);
    }
}
