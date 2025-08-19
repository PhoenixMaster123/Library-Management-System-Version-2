package app.domain.services;

import app.domain.dto.CreateNewCustomer;
import app.domain.models.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.input.CustomerUseCase;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CustomerService implements CustomerUseCase {
    private final CustomerRepositoryPort customerRepositoryPort;

    @Autowired
    public CustomerService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public Customer createNewCustomer(CreateNewCustomer createNewCustomer) {
        Customer customer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
        customerRepositoryPort.saveCustomer(customer);
        return customer;
    }

    @Override
    //@Cacheable(value = "customer", key = "#id")
    public Optional<Customer> findCustomerById(UUID id) {
        return customerRepositoryPort.getCustomer(id);
    }

    @Override
    //@Cacheable(value = "customer", key = "#customerName")
    public Optional<Customer> findCustomerByName(String customerName) {
        return customerRepositoryPort.getCustomerByName(customerName);
    }

    @Override
    public Page<Customer> getPaginatedCustomers(Pageable pageable) {
        return customerRepositoryPort.getPaginatedCustomers(pageable);
    }

    @Override
    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        return customerRepositoryPort.searchCustomer(query, pageable);
    }

    @Override
    //@CachePut(value = "customer", key = "#id")
    public void updatePrivileges(UUID id, boolean privileges) {
        Customer customer = customerRepositoryPort.getCustomer(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        customer.setPrivileges(privileges);
        customerRepositoryPort.updatePrivileges(customer);
    }

    @Override
    //@CachePut(value = "customer", key = "#customer.customerId")
    public void updateCustomer(Customer customer) {
        if (customerRepositoryPort.getCustomer(customer.getCustomerId()).isEmpty()) {
            throw new EntityNotFoundException("Customer not found with ID: " + customer.getCustomerId());
        }
        customerRepositoryPort.updateCustomer(customer);
    }

    @Override
    //@CacheEvict(value = "customer", key = "#id")
    public void deleteCustomer(UUID id) {
        customerRepositoryPort.deleteCustomer(id);
    }
}
