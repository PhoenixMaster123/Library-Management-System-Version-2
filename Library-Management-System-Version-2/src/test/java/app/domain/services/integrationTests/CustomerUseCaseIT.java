package app.domain.services.integrationTests;

import app.domain.dto.CreateNewCustomer;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.model.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.input.CustomerUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public class CustomerUseCaseIT {

        @Autowired
        private CustomerUseCase customerUseCase;

        @Autowired
        private CustomerRepositoryPort customerRepositoryPort;

        @Autowired
        private CustomerRepository customerRepository;

        @Test
        void testCreateNewCustomer_PersistsToDatabase() {
            CreateNewCustomer createNewCustomer = new CreateNewCustomer("John Doe", "john.doe@example.com", true);

            customerUseCase.createNewCustomer(createNewCustomer);

            Optional<Customer> createdCustomer = customerRepositoryPort.getCustomerByName(createNewCustomer.getName());

            assertThat(createdCustomer).isPresent();
            assertThat(createdCustomer.get().getName()).isEqualTo(createNewCustomer.getName());
            assertThat(createdCustomer.get().getEmail()).isEqualTo(createNewCustomer.getEmail());
        }

        @Test
        void testFindCustomerById_FromDatabase() {
            Customer expectedCustomer = new Customer(null, "Jane Doe", "jane.doe@example.com", true);
            customerRepositoryPort.saveCustomer(expectedCustomer);

            Optional<Customer> actualCustomer = customerUseCase.findCustomerById(expectedCustomer.getCustomerId());

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).usingRecursiveComparison().isEqualTo(expectedCustomer);
        }

        @Test
        void testFindCustomerByName_FromDatabase() {
            String customerName = "John Doe";
            Customer expectedCustomer = new Customer(null, customerName, "john.doe@example.com", true);
            customerRepositoryPort.saveCustomer(expectedCustomer);

            Optional<Customer> actualCustomer = customerUseCase.findCustomerByName(customerName);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).usingRecursiveComparison().isEqualTo(expectedCustomer);
        }

        @Test
        void testGetPaginatedCustomers_FromDatabase() {
            Pageable pageable = PageRequest.of(0, 10);
            int numberOfCustomersToCreate = 20;

            for (int i = 0; i < numberOfCustomersToCreate; i++) {
                customerRepositoryPort.saveCustomer(new Customer(null, "Customer " + i, "customer" + i + "@example.com", true));
            }

            Page<Customer> actualPage = customerUseCase.getPaginatedCustomers(pageable);

            assertThat(actualPage.getContent()).hasSize(10);
            assertThat(actualPage.getTotalElements()).isEqualTo(numberOfCustomersToCreate);
            assertThat(actualPage.getTotalPages()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void testSearchCustomer_FromDatabase() {
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);

            customerRepositoryPort.saveCustomer(new Customer(null, "John Doe", "john.doe@example.com", true));
            customerRepositoryPort.saveCustomer(new Customer(null, "Jane Doe", "jane.doe@example.com", true));
            customerRepositoryPort.saveCustomer(new Customer(null, "John Smith", "john.smith@example.com", true));
            customerRepositoryPort.saveCustomer(new Customer(null, "David Lee", "david.lee@example.com", true));

            Page<Customer> actualPage = customerUseCase.searchCustomer(query, pageable);

            assertThat(actualPage.getContent()).hasSize(2);
            assertThat(actualPage.getContent()).anyMatch(c -> c.getName().equals("John Doe"));
            assertThat(actualPage.getContent()).anyMatch(c -> c.getName().equals("John Smith"));
        }

        @Test
        void testUpdatePrivileges_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", false);
            customerRepositoryPort.saveCustomer(customer);
            boolean newPrivileges = true;

            customerUseCase.updatePrivileges(customer.getCustomerId(), newPrivileges);

            Optional<Customer> updatedCustomer = customerRepositoryPort.getCustomer(customer.getCustomerId());
            assertThat(updatedCustomer).isPresent();
            assertThat(updatedCustomer.get().isPrivileges()).isTrue();
        }

        @Test
        void testUpdateCustomer_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
            customerRepositoryPort.saveCustomer(customer);
            customer.setName("Jane Doe");

            customerUseCase.updateCustomer(customer);

            Optional<Customer> updatedCustomer = customerRepositoryPort.getCustomer(customer.getCustomerId());
            assertThat(updatedCustomer).isPresent();
            assertThat(updatedCustomer.get().getName()).isEqualTo("Jane Doe");
        }

        @Test
        void testDeleteCustomer_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
            customerRepositoryPort.saveCustomer(customer);

            customerUseCase.deleteCustomer(customer.getCustomerId());

            Optional<Customer> deletedCustomer = customerRepositoryPort.getCustomer(customer.getCustomerId());
            assertThat(deletedCustomer).isEmpty();
        }
        @AfterEach
        void tearDown() {
            customerRepository.deleteAll();
        }
    }
