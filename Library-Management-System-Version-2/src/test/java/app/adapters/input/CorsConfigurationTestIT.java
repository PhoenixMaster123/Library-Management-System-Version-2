package app.adapters.input;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A separately hosted frontend only works if the preflight is answered for its origin. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "library.cors.allowed-origins=https://example.github.io")
@Tag("integration")
class CorsConfigurationTestIT {

    private static final String ALLOWED = "https://example.github.io";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsThePreflightFromAConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED));
    }

    /**
     * The token travels in this header, so a browser will not send it unless it is allowed.
     * Spring echoes the requested name verbatim, hence the case-insensitive match.
     */
    @Test
    void allowsTheAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/books/paginated")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    @Test
    void refusesAnOriginThatIsNotConfigured() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header(HttpHeaders.ORIGIN, "https://not-mine.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
