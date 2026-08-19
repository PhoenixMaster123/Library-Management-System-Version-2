package app.adapters.input;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.UserRepository;
import app.domain.dto.RegisterRequest;
import app.domain.model.AccountCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class RegistrationControllerTestIT {

    private static final String USERNAME = "new_member";
    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationCreatesAnAccountAndAMembership() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(USERNAME, PASSWORD, "New Member", "new.member@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.customerId").isString());

        UserEntity saved = userRepository.findByUsername(USERNAME).orElseThrow();
        assertEquals("USER", saved.getRole(), "self-registered accounts must never be admins");
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()), "password must be hashed");
        assertNotNull(saved.getCustomerId(), "the account must be linked to a library membership");
        assertTrue(customerRepository.findById(saved.getCustomerId()).isPresent());
    }

    @Test
    void theNewAccountCanSignIn() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(USERNAME, PASSWORD, "New Member", "new.member@example.com"))))
                .andExpect(status().isCreated());

        AccountCredentials credentials = new AccountCredentials();
        credentials.setUsername(USERNAME);
        credentials.setPassword(PASSWORD);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void takenUsernameIsRejected() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterRequest(USERNAME, PASSWORD, "New Member", "new.member@example.com"));

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ab", "123", "", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());

        assertTrue(userRepository.findByUsername("ab").isEmpty());
    }

    @AfterEach
    void tearDown() {
        Optional<UserEntity> user = userRepository.findByUsername(USERNAME);
        user.ifPresent(entity -> {
            if (entity.getCustomerId() != null) {
                customerRepository.deleteById(entity.getCustomerId());
            }
            userRepository.delete(entity);
        });
    }
}
