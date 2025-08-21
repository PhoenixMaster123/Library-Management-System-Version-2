package app.domain.services.unitTests;

import app.domain.model.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.dto.CreateNewCustomer;
import app.domain.services.CustomerService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CustomerUseCaseTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @InjectMocks
    private CustomerService customerService;
        @Test
        void testCreateNewCustomer() {
            CreateNewCustomer createNewCustomer = new CreateNewCustomer("John Doe", "john.doe@example.com", true);

            Customer expectedCustomer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
            doNothing().when(customerRepositoryPort).saveCustomer(argThat(c ->
                    c.getName().equals(expectedCustomer.getName()) &&
                            c.getEmail().equals(expectedCustomer.getEmail()) &&
                            c.isPrivileges() == expectedCustomer.isPrivileges()));

            Customer actualCustomer = customerService.createNewCustomer(createNewCustomer);

            assertThat(actualCustomer.getName()).isEqualTo(expectedCustomer.getName());
            assertThat(actualCustomer.getEmail()).isEqualTo(expectedCustomer.getEmail());
            assertThat(actualCustomer.isPrivileges()).isEqualTo(expectedCustomer.isPrivileges());

            verify(customerRepositoryPort).saveCustomer(argThat(c ->
                    c.getName().equals(expectedCustomer.getName()) &&
                            c.getEmail().equals(expectedCustomer.getEmail()) &&
                            c.isPrivileges() == expectedCustomer.isPrivileges()));
        }
        @Test
        void testFindCustomerById_Found() {
            UUID customerId = UUID.randomUUID();
            Customer expectedCustomer = new Customer(customerId, "Jane Doe", "jane.doe@example.com", true);

            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(expectedCustomer));

            Optional<Customer> actualCustomer = customerService.findCustomerById(customerId);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).isEqualTo(expectedCustomer);
            verify(customerRepositoryPort).getCustomer(customerId);
        }

        @Test
        void testFindCustomerById_NotFound() {
            UUID customerId = UUID.randomUUID();

            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.empty());

            Optional<Customer> actualCustomer = customerService.findCustomerById(customerId);

            assertThat(actualCustomer).isEmpty();
            verify(customerRepositoryPort).getCustomer(customerId);
        }

        @Test
        void testFindCustomerByName_Found() {
            String customerName = "John Doe";
            Customer expectedCustomer = new Customer(UUID.randomUUID(), customerName, "john.doe@example.com", true);
            when(customerRepositoryPort.getCustomerByName(customerName)).thenReturn(Optional.of(expectedCustomer));

            Optional<Customer> actualCustomer = customerService.findCustomerByName(customerName);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).isEqualTo(expectedCustomer);
            verify(customerRepositoryPort).getCustomerByName(customerName);
        }

        @Test
        void testFindCustomerByName_NotFound() {
            String customerName = "Non-existent Customer";
            when(customerRepositoryPort.getCustomerByName(customerName)).thenReturn(Optional.empty());

            Optional<Customer> actualCustomer = customerService.findCustomerByName(customerName);

            assertThat(actualCustomer).isEmpty();
            verify(customerRepositoryPort).getCustomerByName(customerName);
        }

        @Test
        void testGetPaginatedCustomers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> expectedPage = mock(Page.class);
            when(customerRepositoryPort.getPaginatedCustomers(pageable)).thenReturn(expectedPage);

            Page<Customer> actualPage = customerService.getPaginatedCustomers(pageable);

            assertThat(actualPage).isEqualTo(expectedPage);
            verify(customerRepositoryPort).getPaginatedCustomers(pageable);
        }

        @Test
        void testSearchCustomer() {
            // Given
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> expectedPage = mock(Page.class);
            when(customerRepositoryPort.searchCustomer(query, pageable)).thenReturn(expectedPage);

            // When
            Page<Customer> actualPage = customerService.searchCustomer(query, pageable);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            verify(customerRepositoryPort).searchCustomer(query, pageable);
        }

        @Test
        void testUpdatePrivileges_CustomerFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            Customer mockCustomer = mock(Customer.class); // Mock the Customer object
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(mockCustomer));
            boolean newPrivileges = true;

            // When
            customerService.updatePrivileges(customerId, newPrivileges);

            // Then
            verify(customerRepositoryPort).getCustomer(customerId);
            verify(mockCustomer).setPrivileges(newPrivileges); // Verify on the mock
            verify(customerRepositoryPort).updatePrivileges(mockCustomer);
        }

        @Test
        void testUpdatePrivileges_CustomerNotFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.empty());
            boolean newPrivileges = true;

            // When
            assertThrows(EntityNotFoundException.class, () -> customerService.updatePrivileges(customerId, newPrivileges));

            // Then
            verify(customerRepositoryPort).getCustomer(customerId);
            verify(customerRepositoryPort, never()).updatePrivileges(any(Customer.class));
        }

        @Test
        void testUpdateCustomer_CustomerFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.of(customer));

            // When
            customerService.updateCustomer(customer);

            // Then
            verify(customerRepositoryPort).getCustomer(customerId);
            verify(customerRepositoryPort).updateCustomer(customer);
        }

        @Test
        void testUpdateCustomer_CustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
            when(customerRepositoryPort.getCustomer(customerId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> customerService.updateCustomer(customer));

            verify(customerRepositoryPort).getCustomer(customerId);
            verify(customerRepositoryPort, never()).updateCustomer(customer);
        }

        @Test
        void testDeleteCustomer() {
            UUID customerId = UUID.randomUUID();

            customerService.deleteCustomer(customerId);

            verify(customerRepositoryPort).deleteCustomer(customerId);
        }
    }