package app.adapters.input;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.ReminderPreferenceRepository;
import app.adapters.output.repositories.UserRepository;
import app.domain.dto.CreateNewCustomer;
import app.domain.model.Customer;
import app.domain.port.input.CustomerUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The preference has to survive the round trip. It used to live only in Notification-Service and
 * be written best-effort, so with that service down a member was told reminders were on and found
 * the box empty on their next visit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "member")
@Tag("integration")
class ReminderControllerTestIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CustomerUseCase customerUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ReminderPreferenceRepository reminderPreferenceRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Reminder Member", "member@example.com", true));
        userRepository.save(new UserEntity(
                "member", passwordEncoder.encode("secret"), "USER", customer.getCustomerId()));
    }

    @Test
    void reminderStartsOffAndReportsTheMembershipEmail() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(true))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.email").value("member@example.com"));
    }

    /** The bug this endpoint was rewritten for: switching reminders on has to still be on later. */
    @Test
    void switchingRemindersOnSurvivesTheNextRequest() throws Exception {
        mockMvc.perform(put("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.email").value("member@example.com"));
    }

    @Test
    void switchingRemindersBackOffAlsoSticks() throws Exception {
        mockMvc.perform(put("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/reminders"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    /**
     * Reminders go to the address on the membership. A caller who sends one of their own must not
     * be able to redirect them.
     */
    @Test
    void anEmailInTheBodyIsIgnored() throws Exception {
        mockMvc.perform(put("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true, \"email\": \"attacker@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"));
    }

    /** Staff accounts hold no membership, so there is nothing to remind. */
    @Test
    @WithMockUser(username = "desk")
    void anAccountWithoutMembershipIsUnsupported() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(false));

        mockMvc.perform(put("/api/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest());
    }

    @AfterEach
    void tearDown() {
        reminderPreferenceRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
