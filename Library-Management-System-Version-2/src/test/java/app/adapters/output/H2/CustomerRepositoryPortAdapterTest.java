package app.adapters.output.H2;

import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.entity.TransactionEntity;
import app.adapters.output.CustomerRepositoryPortAdapter;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CustomerRepositoryPortAdapterTest {

    @Mock
    private CustomerRepository customerRepository;
    @InjectMocks
    private CustomerRepositoryPortAdapter customerDaoAdapter;

    @BeforeEach
    public void setUp() {
        customerDaoAdapter = new CustomerRepositoryPortAdapter(customerRepository);
    }

    @Test
    public void testSaveCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);
        CustomerEntity customerEntity = new CustomerEntity();

        customerEntity.setCustomerId(customerId);
        customerEntity.setName(customer.getName());
        customerEntity.setEmail(customer.getEmail());
        customerEntity.setPrivileges(customer.isPrivileges());

        Mockito.when(customerRepository.save(Mockito.any(CustomerEntity.class))).thenReturn(customerEntity);

        customerDaoAdapter.saveCustomer(customer);

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        Mockito.verify(customerRepository).save(captor.capture());

        CustomerEntity capturedEntity = captor.getValue();
        assertNotNull(capturedEntity, "Captured CustomerEntity should not be null");
        assertEquals(customer.getName(), capturedEntity.getName());
        assertEquals(customer.getEmail(), capturedEntity.getEmail());
        assertEquals(customer.isPrivileges(), capturedEntity.isPrivileges());
    }

    @Test
    public void testGetPaginatedCustomers() {
        Pageable pageable = Pageable.ofSize(10);

        List<CustomerEntity> customerEntities = new ArrayList<>();
        customerEntities.add(new CustomerEntity(UUID.randomUUID(), "John Doe", "john.doe@example.com", true, new ArrayList<>()));
        customerEntities.add(new CustomerEntity(UUID.randomUUID(), "Jane Smith", "jane.smith@example.com", false, new ArrayList<>()));

        Page<CustomerEntity> customerEntityPage = new PageImpl<>(customerEntities, pageable, 2);

        Mockito.when(customerRepository.findAll(pageable)).thenReturn(customerEntityPage);

        Page<Customer> customers = customerDaoAdapter.getPaginatedCustomers(pageable);

        assertEquals(2, customers.getTotalElements());
        assertEquals("John Doe", customers.getContent().get(0).getName());
        assertEquals("jane.smith@example.com", customers.getContent().get(1).getEmail());

        Mockito.verify(customerRepository).findAll(pageable);
    }

    @Test
    public void testSearchCustomer() {
        String query = "john";
        Pageable pageable = Pageable.ofSize(10);

        List<CustomerEntity> customerEntities = new ArrayList<>();
        customerEntities.add(new CustomerEntity(UUID.randomUUID(), "John Doe", "john.doe@example.com", true, new ArrayList<>()));

        Page<CustomerEntity> customerEntityPage = new PageImpl<>(customerEntities, pageable, 1);

        Mockito.when(customerRepository.searchByQuery(query.toLowerCase(), pageable)).thenReturn(customerEntityPage);

        Page<Customer> customers = customerDaoAdapter.searchCustomer(query, pageable);

        assertEquals(1, customers.getTotalElements());
        assertEquals("John Doe", customers.getContent().getFirst().getName());

        Mockito.verify(customerRepository).searchByQuery(query.toLowerCase(), pageable);
    }

    @Test
    public void testGetCustomerById_Found() {
        UUID customerId = UUID.randomUUID();
        CustomerEntity customerEntity = new CustomerEntity(customerId, "John Doe", "john.doe@example.com", true, new ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));

        Optional<Customer> customer = customerDaoAdapter.getCustomer(customerId);

        assertTrue(customer.isPresent());
        assertEquals(customerId, customer.get().getCustomerId());
        assertEquals("John Doe", customer.get().getName());

        Mockito.verify(customerRepository).findById(customerId);
    }

    @Test
    public void testGetCustomerById_NotFound() {
        UUID customerId = UUID.randomUUID();

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        Optional<Customer> customer = customerDaoAdapter.getCustomer(customerId);

        assertFalse(customer.isPresent());

        Mockito.verify(customerRepository).findById(customerId);
    }

    @Test
    public void testGetCustomerByName_Found() {
        String name = "John Doe";
        UUID transactionId = UUID.randomUUID();
        TransactionEntity transaction = new TransactionEntity(transactionId, LocalDate.now(), LocalDate.now(), LocalDate.ofYearDay(2025,1), null, null);

        CustomerEntity customerEntity = new CustomerEntity(
                UUID.randomUUID(), name, "john.doe@example.com", true, new ArrayList<>());
        customerEntity.getTransactions().add(transaction);

        Mockito.when(customerRepository.findByName(name)).thenReturn(Optional.of(customerEntity));

        Optional<Customer> customer = customerDaoAdapter.getCustomerByName(name);

        assertTrue(customer.isPresent());
        assertEquals(name, customer.get().getName());

        List<Transaction> transactions = customer.get().getTransactions();
        assertEquals(1, transactions.size());
        assertEquals(transactionId, transactions.getFirst().getTransactionId());

        Mockito.verify(customerRepository).findByName(name);
    }

    @Test
    public void testGetCustomerByName_NotFound() {
        String name = "John Doe";

        Mockito.when(customerRepository.findByName(name)).thenReturn(Optional.empty());

        Optional<Customer> customer = customerDaoAdapter.getCustomerByName(name);

        assertFalse(customer.isPresent());

        Mockito.verify(customerRepository).findByName(name);
    }

    @Test
    public void testUpdatePrivileges_Found() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", false);

        CustomerEntity customerEntity = new CustomerEntity(customerId, customer.getName(), customer.getEmail(), customer.isPrivileges(), new ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));

        customerDaoAdapter.updatePrivileges(customer);

        Mockito.verify(customerRepository).findById(customerId);
        Mockito.verify(customerRepository).save(customerEntity);
        assertFalse(customerEntity.isPrivileges());
    }

    @Test
    public void testUpdatePrivileges_NotFound() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId, "John Doe", "john.doe@example.com", true);

        assertThrows(EntityNotFoundException.class, () -> customerDaoAdapter.updatePrivileges(customer));

        Mockito.verify(customerRepository).findById(customerId);
    }

    @Test
    public void testUpdateCustomer_Found() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId, "Updated Name", "updated@example.com", true);

        CustomerEntity customerEntity = new CustomerEntity(customerId, "Old Name", "old@example.com", false, new ArrayList<>());

        Mockito.when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));

        customerDaoAdapter.updateCustomer(customer);

        Mockito.verify(customerRepository).findById(customerId);
        Mockito.verify(customerRepository).save(customerEntity);
        assertEquals("Updated Name", customerEntity.getName());
        assertEquals("updated@example.com", customerEntity.getEmail());
        assertTrue(customerEntity.isPrivileges());
    }

    @Test
    public void testUpdateCustomer_NotFound() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(customerId, "Updated Name", "updated@example.com", true);

        assertThrows(EntityNotFoundException.class, () -> customerDaoAdapter.updateCustomer(customer));

        Mockito.verify(customerRepository).findById(customerId);
    }

    @Test
    public void testDeleteCustomer_Found() {
        UUID customerId = UUID.randomUUID();

        Mockito.when(customerRepository.existsById(customerId)).thenReturn(true);

        customerDaoAdapter.deleteCustomer(customerId);

        Mockito.verify(customerRepository).existsById(customerId);
        Mockito.verify(customerRepository).deleteById(customerId);
    }

    @Test
    public void testDeleteCustomer_NotFound() {
        UUID customerId = UUID.randomUUID();

        Mockito.when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> customerDaoAdapter.deleteCustomer(customerId));

        Mockito.verify(customerRepository).existsById(customerId);
    }
}