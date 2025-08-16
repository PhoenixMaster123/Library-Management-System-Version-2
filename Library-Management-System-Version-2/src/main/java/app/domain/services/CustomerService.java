package app.domain.services;

import app.domain.dto.CreateNewCustomer;
import app.domain.models.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.input.CustomerUseCase;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Customer createNewCustomer(CreateNewCustomer createNewCustomer) {
        Customer customer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
        customerRepositoryPort.saveCustomer(customer);
        return customer;
    }

    public Optional<Customer> findCustomerById(UUID id) {
        return customerRepositoryPort.getCustomer(id);
    }

    public Optional<Customer> findCustomerByName(String customerName) {
        return customerRepositoryPort.getCustomerByName(customerName);
    }

    public Page<Customer> getPaginatedCustomers(Pageable pageable) {
        return customerRepositoryPort.getPaginatedCustomers(pageable);
    }

    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        return customerRepositoryPort.searchCustomer(query, pageable);
    }

    public void updatePrivileges(UUID id, boolean privileges) {
        Customer customer = findCustomerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        customer.setPrivileges(privileges);
        customerRepositoryPort.updatePrivileges(customer);
    }

    public void updateCustomer(Customer customer) {
        if (findCustomerById(customer.getCustomerId()).isEmpty()) {
            throw new EntityNotFoundException("Customer not found with ID: " + customer.getCustomerId());
        }
        customerRepositoryPort.updateCustomer(customer);
    }

    public void deleteCustomer(UUID id) {
        customerRepositoryPort.deleteCustomer(id);
    }
}
