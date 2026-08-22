package app.domain.services;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers signed-out tokens, so a bearer token stops working at sign-out rather than at expiry. */
@Service
public class TokenRevocationService {

    /** Token id to the moment it expires, after which remembering it serves no purpose. */
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    /** Marks a token signed out until the moment it would have expired anyway. */
    public void revoke(Claims claims) {
        if (claims == null) {
            return;
        }
        String id = claims.getId();
        if (id == null) {
            return;
        }
        purgeExpired();
        revoked.put(id, claims.getExpiration() == null ? Instant.now() : claims.getExpiration().toInstant());
    }

    /** True while the token is on the list; drops the entry once it would have expired. */
    public boolean isRevoked(Claims claims) {
        if (claims == null || claims.getId() == null) {
            return false;
        }
        Instant expiry = revoked.get(claims.getId());
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            revoked.remove(claims.getId());
            return false;
        }
        return true;
    }

    /** Drops entries for tokens that have expired, so the list stays small. */
    private void purgeExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
