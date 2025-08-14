package app.domain.services.unitTests;

import app.domain.models.Customer;
import app.domain.port.CustomerDao;
import app.adapters.in.dto.CreateNewCustomer;
import app.domain.services.impl.CustomerServiceImpl;
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
public class CustomerServiceTest {

    @Mock
    private CustomerDao customerDao;

    @InjectMocks
    private CustomerServiceImpl customerService;
        @Test
        void testCreateNewCustomer() {
            CreateNewCustomer createNewCustomer = new CreateNewCustomer("John Doe", "john.doe@example.com", true);

            Customer expectedCustomer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
            doNothing().when(customerDao).addCustomer(argThat(c ->
                    c.getName().equals(expectedCustomer.getName()) &&
                            c.getEmail().equals(expectedCustomer.getEmail()) &&
                            c.isPrivileges() == expectedCustomer.isPrivileges()));

            Customer actualCustomer = customerService.createNewCustomer(createNewCustomer);

            assertThat(actualCustomer.getName()).isEqualTo(expectedCustomer.getName());
            assertThat(actualCustomer.getEmail()).isEqualTo(expectedCustomer.getEmail());
            assertThat(actualCustomer.isPrivileges()).isEqualTo(expectedCustomer.isPrivileges());

            verify(customerDao).addCustomer(argThat(c ->
                    c.getName().equals(expectedCustomer.getName()) &&
                            c.getEmail().equals(expectedCustomer.getEmail()) &&
                            c.isPrivileges() == expectedCustomer.isPrivileges()));
        }
        @Test
        void testFindCustomerById_Found() {
            UUID customerId = UUID.randomUUID();
            Customer expectedCustomer = new Customer(customerId, "Jane Doe", "jane.doe@example.com", true);

            when(customerDao.getCustomer(customerId)).thenReturn(Optional.of(expectedCustomer));

            Optional<Customer> actualCustomer = customerService.findCustomerById(customerId);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).isEqualTo(expectedCustomer);
            verify(customerDao).getCustomer(customerId);
        }

        @Test
        void testFindCustomerById_NotFound() {
            UUID customerId = UUID.randomUUID();

            when(customerDao.getCustomer(customerId)).thenReturn(Optional.empty());

            Optional<Customer> actualCustomer = customerService.findCustomerById(customerId);

            assertThat(actualCustomer).isEmpty();
            verify(customerDao).getCustomer(customerId);
        }

        @Test
        void testFindCustomerByName_Found() {
            String customerName = "John Doe";
            Customer expectedCustomer = new Customer(UUID.randomUUID(), customerName, "john.doe@example.com", true);
            when(customerDao.getCustomerByName(customerName)).thenReturn(Optional.of(expectedCustomer));

            Optional<Customer> actualCustomer = customerService.findCustomerByName(customerName);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).isEqualTo(expectedCustomer);
            verify(customerDao).getCustomerByName(customerName);
        }

        @Test
        void testFindCustomerByName_NotFound() {
            String customerName = "Non-existent Customer";
            when(customerDao.getCustomerByName(customerName)).thenReturn(Optional.empty());

            Optional<Customer> actualCustomer = customerService.findCustomerByName(customerName);

            assertThat(actualCustomer).isEmpty();
            verify(customerDao).getCustomerByName(customerName);
        }

        @Test
        void testGetPaginatedCustomers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> expectedPage = mock(Page.class);
            when(customerDao.getPaginatedCustomers(pageable)).thenReturn(expectedPage);

            Page<Customer> actualPage = customerService.getPaginatedCustomers(pageable);

            assertThat(actualPage).isEqualTo(expectedPage);
            verify(customerDao).getPaginatedCustomers(pageable);
        }

        @Test
        void testSearchCustomer() {
            // Given
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> expectedPage = mock(Page.class);
            when(customerDao.searchCustomer(query, pageable)).thenReturn(expectedPage);

            // When
            Page<Customer> actualPage = customerService.searchCustomer(query, pageable);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            verify(customerDao).searchCustomer(query, pageable);
        }

        @Test
        void testUpdatePrivileges_CustomerFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            Customer mockCustomer = mock(Customer.class); // Mock the Customer object
            when(customerDao.getCustomer(customerId)).thenReturn(Optional.of(mockCustomer));
            boolean newPrivileges = true;

            // When
            customerService.updatePrivileges(customerId, newPrivileges);

            // Then
            verify(customerDao).getCustomer(customerId);
            verify(mockCustomer).setPrivileges(newPrivileges); // Verify on the mock
            verify(customerDao).updatePrivileges(mockCustomer);
        }

        @Test
        void testUpdatePrivileges_CustomerNotFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            when(customerDao.getCustomer(customerId)).thenReturn(Optional.empty());
            boolean newPrivileges = true;

            // When
            assertThrows(EntityNotFoundException.class, () -> customerService.updatePrivileges(customerId, newPrivileges));

            // Then
            verify(customerDao).getCustomer(customerId);
            verify(customerDao, never()).updatePrivileges(any(Customer.class));
        }

        @Test
        void testUpdateCustomer_CustomerFound() {
            // Given
            UUID customerId = UUID.randomUUID();
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
            when(customerDao.getCustomer(customerId)).thenReturn(Optional.of(customer));

            // When
            customerService.updateCustomer(customer);

            // Then
            verify(customerDao).getCustomer(customerId);
            verify(customerDao).updateCustomer(customer);
        }

        @Test
        void testUpdateCustomer_CustomerNotFound() {
            UUID customerId = UUID.randomUUID();
            Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
            when(customerDao.getCustomer(customerId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> customerService.updateCustomer(customer));

            verify(customerDao).getCustomer(customerId);
            verify(customerDao, never()).updateCustomer(customer);
        }

        @Test
        void testDeleteCustomer() {
            UUID customerId = UUID.randomUUID();

            customerService.deleteCustomer(customerId);

            verify(customerDao).deleteCustomer(customerId);
        }
    }