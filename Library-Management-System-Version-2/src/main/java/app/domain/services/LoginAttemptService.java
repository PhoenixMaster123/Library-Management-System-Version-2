package app.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Counts failed sign-ins per username and locks the account out for a while. In memory only. */
@Service
@Slf4j
public class LoginAttemptService {

    /** How many failures, when they started, and until when the account stays locked. */
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

    /** True while the account is locked; clears the entry once the lockout has passed. */
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

    /** Counts one failed sign-in, locking the account once the limit is hit inside the window. */
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

    /** Usernames are counted case-insensitively, so changing case cannot dodge the counter. */
    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(java.util.Locale.ROOT);
    }
}
