package app.adapters.input;

import app.domain.dto.CreateNewCustomer;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.models.Customer;
import app.domain.port.input.CustomerUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("integration")
class CustomerControllerTestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CustomerUseCase customerUseCase;
    @Test
    void testCreateNewCustomer() throws Exception {
        CreateNewCustomer newCustomer =
                new CreateNewCustomer("Test Customer", "test@example.com", true);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Customer"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.privileges").value(true));
    }

    @Test
    void testCreateNewCustomerWithInvalidEmail() throws Exception {
        CreateNewCustomer newCustomer =
                new CreateNewCustomer("Test Customer", "test", true);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void createNewCustomer_ShouldReturnBadRequest_WhenInputIsInvalid() throws Exception {
        CreateNewCustomer invalidCustomer = new CreateNewCustomer("Test Customer", "test", true);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.email").value("Email should be valid"));
    }
    @Test
    void testCreateNewCustomerWithInvalidName() throws Exception {
        CreateNewCustomer newCustomer =
                new CreateNewCustomer("", "test@example.com", true);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testGetCustomerById_Success() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "test@example.com", true));

        mockMvc.perform(get("/customers/{id}", customer.getCustomerId())
                        .accept("application/single-customer-response+json;version=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/single-customer-response+json;version=1"))
                .andExpect(jsonPath("$.message").value("Customer retrieved successfully"))
                .andExpect(jsonPath("$.data.customerId").value(customer.getCustomerId().toString()))
                .andExpect(jsonPath("$.data.name").value(customer.getName()))
                .andExpect(jsonPath("$.data.email").value(customer.getEmail()));
    }

    @Test
    void testGetCustomerById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/customers/{id}", nonExistentId)
                        .accept("application/single-customer-response+json;version=1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.customerId").value(nonExistentId.toString()));
    }

    @Test
    void testGetCustomerById_InvalidUUID() throws Exception {
        String invalidId = "123-invalid-uuid";

        mockMvc.perform(get("/customers/{id}", invalidId)
                        .accept("application/single-customer-response+json;version=1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"))
                .andExpect(jsonPath("$.providedId").value(invalidId));
    }

    @Test
    void testUpdateCustomer() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "test@example.com", true));

        Customer customerToUpdate = new Customer(
                "Update Customer", "update@example.com",
                false);



        mockMvc.perform(put("/customers/" + customer.getCustomerId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerToUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().string("Customer updated successfully!"));

        assertEquals("Update Customer", customerRepository.findById(customer.getCustomerId()).get().getName());
        assertEquals("update@example.com", customerRepository.findById(customer.getCustomerId()).get().getEmail());
        assertFalse(customerRepository.findById(customer.getCustomerId()).get().isPrivileges());
    }

    @Test
    void testUpdateCustomerPrivileges() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "@example.com", true));

        mockMvc.perform(put("/customers/" + customer.getCustomerId() + "/privileges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("false"))
                .andExpect(status().isOk())
                .andExpect(content().string("Customer privileges updated successfully!"));

        assertFalse(customerRepository.findById(customer.getCustomerId()).get().isPrivileges());
    }
    @Test
    void updateCustomerPrivileges_ShouldReturnBadRequest_WhenInputIsInvalid() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Test Customer", "test@example.com", true));

        mockMvc.perform(put("/customers/" + customer.getCustomerId() + "/privileges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string("Invalid privileges value"));
    }
    @Test
    void testDeleteCustomer() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "test@example.com", true));

        mockMvc.perform(delete("/customers/" + customer.getCustomerId()))
                .andExpect(status().isOk())
                .andExpect(content().string("Customer successfully deleted!"));

        assertFalse(customerRepository.findById(customer.getCustomerId()).isPresent());
    }
    @Test
    void testSearchCustomerByID() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "test@example.com", true));

        mockMvc.perform(get("/customers/search")
                        .param("id", customer.getCustomerId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("data.name").value("Test Customer"))
                .andExpect(jsonPath("data.email").value("test@example.com"));

    }
    @Test
    void testSearchCustomerByID_NotFound() throws Exception {
        mockMvc.perform(get("/customers/search")
                        .param("id", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("Customer not found"));
    }
    @Test
    void testSearchCustomerByName() throws Exception {
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(
                        "Test Customer", "test@example.com", true));

        mockMvc.perform(get("/customers/search")
                        .param("name", customer.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("data.name").value("Test Customer"))
                .andExpect(jsonPath("data.email").value("test@example.com"));

    }
    @Test
    void testSearchCustomerByName_NotFound() throws Exception {
        mockMvc.perform(get("/customers/search")
                        .param("name", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("Customer with the given name not found"));
    }
    @Test
    void testSearchCustomerByQuery_NoResults() throws Exception {
        mockMvc.perform(get("/customers/search")
                        .param("query", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No customers found for the given query"));
    }
    @Test
    void testSearchCustomerByQuery_NoCriteriaProvided() throws Exception {
        mockMvc.perform(get("/customers/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No search criteria provided"));
    }
    @Test
    void testGetAllCustomers() throws Exception {
        mockMvc.perform(get("/customers/paginated")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/customers/paginated?page=0&size=5")))
                .andExpect(header().string("next", Matchers.containsString("/customers/paginated?page=1&size=5")))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("Alice Smith"))
                .andExpect(jsonPath("$.data[1].name").value("Bob Johnson"))
                .andExpect(jsonPath("$.data[2].name").value("Charlie Brown"))
                .andExpect(jsonPath("$.data[3].name").value("Diana Prince"))
                .andExpect(jsonPath("$.data[4].name").value("Ethan Hunt"));

        mockMvc.perform(get("/customers/paginated")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/customers/paginated?page=1&size=5")))
                .andExpect(header().string("prev", Matchers.containsString("/customers/paginated?page=0&size=5")))
                .andExpect(header().doesNotExist("next"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("Fiona Gallagher"))
                .andExpect(jsonPath("$.data[1].name").value("George Bailey"))
                .andExpect(jsonPath("$.data[2].name").value("Hannah Abbott"))
                .andExpect(jsonPath("$.data[3].name").value("Ivan Drago"))
                .andExpect(jsonPath("$.data[4].name").value("Julia Roberts"));
    }
    @AfterEach
    void tearDown() {
        customerRepository.deleteAll();
    }
}