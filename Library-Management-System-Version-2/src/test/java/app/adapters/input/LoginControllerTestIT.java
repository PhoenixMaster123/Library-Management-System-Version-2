package app.adapters.input;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import app.domain.model.AccountCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("integration")
class LoginControllerTestIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        if (userRepository.findByUsername("user").isEmpty()) {
            userRepository.save(new UserEntity("user", passwordEncoder.encode("user"), "USER"));
        }
    }

    @Test
    public void testLogin_Success() throws Exception {
        AccountCredentials validCredentials = new AccountCredentials();
        validCredentials.setUsername("user");
        validCredentials.setPassword("user");

        ResultActions result = mockMvc.perform(
                post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCredentials))
        );

        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, containsString("Bearer ")))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        AccountCredentials invalidCredentials = new AccountCredentials();
        invalidCredentials.setUsername("invalid_user");
        invalidCredentials.setPassword("wrong_password");

        ResultActions result = mockMvc.perform(
                post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCredentials))
        );

        result.andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }
    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }
}