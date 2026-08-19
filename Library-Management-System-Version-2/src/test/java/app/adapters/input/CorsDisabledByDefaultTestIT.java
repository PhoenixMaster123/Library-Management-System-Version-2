package app.adapters.input;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/** With no origins configured, nothing is allowed cross-origin. */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class CorsDisabledByDefaultTestIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void doesNotAllowAnArbitraryOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header(HttpHeaders.ORIGIN, "https://somewhere.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
