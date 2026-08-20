package app.domain.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/** Issues and verifies the tokens the API authenticates with. */
@Component
@Slf4j
public class JwtService {
    static final long EXPIRATION_TIME = 86400000; // 1 day in ms

    static final String PREFIX = "Bearer ";
    static final String ROLE_CLAIM = "role";
    static final String DEFAULT_ROLE = "USER";

    /**
     * Minimum key length for HS256. A shorter secret is rejected by jjwt rather than silently
     * weakening the signature.
     */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    /**
     * Derives the signing key from {@code library.jwt.secret}.
     *
     * <p>Blank means a new random key per start-up, which invalidates every token already issued.
     */
    public JwtService(@Value("${library.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            this.key = Jwts.SIG.HS256.key().build();
            log.info("No library.jwt.secret configured; signing tokens with a key generated for "
                    + "this start-up. Sessions will not survive a restart.");
            return;
        }

        byte[] material = secret.getBytes(StandardCharsets.UTF_8);
        if (material.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("library.jwt.secret must be at least "
                    + MIN_SECRET_BYTES + " characters; it was " + material.length + ".");
        }

        this.key = Keys.hmacShaKeyFor(material);
        log.info("Signing tokens with the configured library.jwt.secret; sessions survive restarts.");
    }

    /**
     * The role travels inside the token so the filter can rebuild the authorities without a
     * database round trip; a token therefore keeps its old role until it expires.
     */
    public String getToken(String username, String role) {
        return Jwts.builder()
                // An id, so a token can be named in the revocation list when its owner signs out.
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    /** Returns the claims of a valid token, or null when the header is missing, malformed or expired. */
    public Claims getClaims(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith(PREFIX)) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.substring(PREFIX.length()))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid or expired JWT: {}", e.getMessage());
            return null;
        }
    }

    /** Falls back to USER so a token without the claim cannot silently gain privileges. */
    public String getRole(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);
        return (role == null || role.isBlank()) ? DEFAULT_ROLE : role;
    }
}
