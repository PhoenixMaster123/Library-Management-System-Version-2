package app.domain.services;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers tokens that have been signed out, so a bearer token stops working the moment its owner
 * says so rather than when it happens to expire.
 *
 * <p>A signed JWT is valid until its expiry by design: nothing about it is looked up, which is what
 * makes it cheap. The cost is that signing out can only ever be a client-side gesture unless the
 * server keeps a list like this one, so a copied token would keep working for the rest of the day.
 *
 * <p>Entries are dropped once the token would have expired anyway, so the list stays the size of
 * however many people signed out recently. It is in memory: a restart forgets it, but a restart
 * also mints a new signing key unless one is configured, which invalidates everything regardless.
 */
@Service
public class TokenRevocationService {

    /** Token id to the moment it expires, after which remembering it serves no purpose. */
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

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

    private void purgeExpired() {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
