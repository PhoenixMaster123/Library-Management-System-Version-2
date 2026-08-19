package app.domain.services.unitTests;

import app.domain.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class JwtServiceTest {

    private static final String SECRET = "a-test-signing-key-that-is-long-enough";

    private static HttpServletRequest requestBearing(String token) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        return request;
    }

    /**
     * The reason the secret is configurable at all: a restart used to invalidate every token,
     * signing everyone out on each devtools reload. A second instance stands in for that restart.
     */
    @Test
    void aTokenSurvivesARestartWhenTheSecretIsConfigured() {
        String token = new JwtService(SECRET).getToken("ada", "ADMIN");

        JwtService afterRestart = new JwtService(SECRET);
        Claims claims = afterRestart.getClaims(requestBearing("Bearer " + token));

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("ada");
        assertThat(afterRestart.getRole(claims)).isEqualTo("ADMIN");
    }

    /** Without a configured secret the old behaviour stands: each start-up gets its own key. */
    @Test
    void aTokenDoesNotSurviveARestartWithoutAConfiguredSecret() {
        String token = new JwtService("").getToken("ada", "ADMIN");

        assertThat(new JwtService("").getClaims(requestBearing("Bearer " + token))).isNull();
    }

    @Test
    void rejectsASecretTooShortToSignWith() {
        assertThatThrownBy(() -> new JwtService("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void refusesATokenSignedWithADifferentSecret() {
        String token = new JwtService(SECRET).getToken("ada", "ADMIN");

        JwtService other = new JwtService("a-completely-different-key-of-sufficient-length");

        assertThat(other.getClaims(requestBearing("Bearer " + token))).isNull();
    }

    @Test
    void ignoresAMissingOrMalformedHeader() {
        JwtService service = new JwtService(SECRET);

        assertThat(service.getClaims(requestBearing(null))).isNull();
        assertThat(service.getClaims(requestBearing("not-a-bearer-token"))).isNull();
    }

    /** A token with no role claim must not be able to slip through as an administrator. */
    @Test
    void fallsBackToUserWhenTheRoleClaimIsMissing() {
        JwtService service = new JwtService(SECRET);
        Claims claims = service.getClaims(requestBearing("Bearer " + service.getToken("ada", null)));

        assertThat(service.getRole(claims)).isEqualTo("USER");
    }
}
