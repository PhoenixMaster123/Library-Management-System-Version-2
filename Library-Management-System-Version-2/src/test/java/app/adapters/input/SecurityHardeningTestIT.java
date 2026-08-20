package app.adapters.input;

import app.domain.model.AccountCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The protections that stop a token or a password being worn down by repetition. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "library.admin.password=known-admin-password",
        "library.login.max-attempts=3",
        "library.login.lockout=PT5M",
})
@Tag("integration")
class SecurityHardeningTestIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String loginBody(String username, String password) throws Exception {
        AccountCredentials credentials = new AccountCredentials();
        credentials.setUsername(username);
        credentials.setPassword(password);
        return objectMapper.writeValueAsString(credentials);
    }

    private String tokenFor(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    /** Without a limit the password is the only obstacle, and it can be tried at network speed. */
    @Test
    void locksTheAccountAfterRepeatedFailures() throws Exception {
        String username = "lockme";

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(username, "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "wrong")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    /** A lockout is per account, or one attacker could shut the whole library out. */
    @Test
    void lockingOneAccountLeavesAnotherAlone() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("victim", "wrong")));
        }

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "known-admin-password")))
                .andExpect(status().isOk());
    }

    /** Signing out has to mean something server-side, or a copied token works until it expires. */
    @Test
    void aRevokedTokenStopsWorking() throws Exception {
        String token = tokenFor("admin", "known-admin-password");

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/revoke").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    /** A second token must survive the first one being revoked. */
    @Test
    void revokingOneTokenLeavesAnotherValid() throws Exception {
        String first = tokenFor("admin", "known-admin-password");
        String second = tokenFor("admin", "known-admin-password");

        mockMvc.perform(post("/api/revoke").header("Authorization", first)).andExpect(status().isOk());

        mockMvc.perform(get("/api/me").header("Authorization", second)).andExpect(status().isOk());
    }

    @Test
    void sendsAContentSecurityPolicy() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "known-admin-password")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Referrer-Policy", containsString("same-origin")));
    }

    /**
     * An unknown path is not a server error, and must not describe our internals.
     *
     * <p>Signed in on purpose: an unknown path under /api is refused by the filter chain as a 401
     * before any handler sees it, which is right - this checks what happens once past that.
     */
    @Test
    void answersAnUnknownPathWithNotFound() throws Exception {
        String token = tokenFor("admin", "known-admin-password");

        mockMvc.perform(get("/api/there-is-nothing-here").header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
