package app.domain.services.integrationTests;

import app.adapters.in.dto.CreateNewCustomer;
import app.adapters.out.H2.repositories.CustomerRepository;
import app.domain.models.Customer;
import app.domain.port.CustomerDao;
import app.domain.services.CustomerService;
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
public class CustomerServiceIT {

        @Autowired
        private CustomerService customerService;

        @Autowired
        private CustomerDao customerDao;

        @Autowired
        private CustomerRepository customerRepository;

        @Test
        void testCreateNewCustomer_PersistsToDatabase() {
            CreateNewCustomer createNewCustomer = new CreateNewCustomer("John Doe", "john.doe@example.com", true);

            customerService.createNewCustomer(createNewCustomer);

            Optional<Customer> createdCustomer = customerDao.getCustomerByName(createNewCustomer.getName());

            assertThat(createdCustomer).isPresent();
            assertThat(createdCustomer.get().getName()).isEqualTo(createNewCustomer.getName());
            assertThat(createdCustomer.get().getEmail()).isEqualTo(createNewCustomer.getEmail());
        }

        @Test
        void testFindCustomerById_FromDatabase() {
            Customer expectedCustomer = new Customer(null, "Jane Doe", "jane.doe@example.com", true);
            customerDao.addCustomer(expectedCustomer);

            Optional<Customer> actualCustomer = customerService.findCustomerById(expectedCustomer.getCustomerId());

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).usingRecursiveComparison().isEqualTo(expectedCustomer);
        }

        @Test
        void testFindCustomerByName_FromDatabase() {
            String customerName = "John Doe";
            Customer expectedCustomer = new Customer(null, customerName, "john.doe@example.com", true);
            customerDao.addCustomer(expectedCustomer);

            Optional<Customer> actualCustomer = customerService.findCustomerByName(customerName);

            assertThat(actualCustomer).isPresent();
            assertThat(actualCustomer.get()).usingRecursiveComparison().isEqualTo(expectedCustomer);
        }

        @Test
        void testGetPaginatedCustomers_FromDatabase() {
            Pageable pageable = PageRequest.of(0, 10);
            int numberOfCustomersToCreate = 20;

            for (int i = 0; i < numberOfCustomersToCreate; i++) {
                customerDao.addCustomer(new Customer(null, "Customer " + i, "customer" + i + "@example.com", true));
            }

            Page<Customer> actualPage = customerService.getPaginatedCustomers(pageable);

            assertThat(actualPage.getContent()).hasSize(10);
            assertThat(actualPage.getTotalElements()).isEqualTo(numberOfCustomersToCreate);
            assertThat(actualPage.getTotalPages()).isGreaterThanOrEqualTo(2);
        }

        @Test
        void testSearchCustomer_FromDatabase() {
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);

            customerDao.addCustomer(new Customer(null, "John Doe", "john.doe@example.com", true));
            customerDao.addCustomer(new Customer(null, "Jane Doe", "jane.doe@example.com", true));
            customerDao.addCustomer(new Customer(null, "John Smith", "john.smith@example.com", true));
            customerDao.addCustomer(new Customer(null, "David Lee", "david.lee@example.com", true));

            Page<Customer> actualPage = customerService.searchCustomer(query, pageable);

            assertThat(actualPage.getContent()).hasSize(2);
            assertThat(actualPage.getContent()).anyMatch(c -> c.getName().equals("John Doe"));
            assertThat(actualPage.getContent()).anyMatch(c -> c.getName().equals("John Smith"));
        }

        @Test
        void testUpdatePrivileges_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", false);
            customerDao.addCustomer(customer);
            boolean newPrivileges = true;

            customerService.updatePrivileges(customer.getCustomerId(), newPrivileges);

            Optional<Customer> updatedCustomer = customerDao.getCustomer(customer.getCustomerId());
            assertThat(updatedCustomer).isPresent();
            assertThat(updatedCustomer.get().isPrivileges()).isTrue();
        }

        @Test
        void testUpdateCustomer_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
            customerDao.addCustomer(customer);
            customer.setName("Jane Doe");

            customerService.updateCustomer(customer);

            Optional<Customer> updatedCustomer = customerDao.getCustomer(customer.getCustomerId());
            assertThat(updatedCustomer).isPresent();
            assertThat(updatedCustomer.get().getName()).isEqualTo("Jane Doe");
        }

        @Test
        void testDeleteCustomer_FromDatabase() {
            Customer customer = new Customer(null, "John Doe", "john.doe@example.com", true);
            customerDao.addCustomer(customer);

            customerService.deleteCustomer(customer.getCustomerId());

            Optional<Customer> deletedCustomer = customerDao.getCustomer(customer.getCustomerId());
            assertThat(deletedCustomer).isEmpty();
        }
        @AfterEach
        void tearDown() {
            customerRepository.deleteAll();
        }
    }
