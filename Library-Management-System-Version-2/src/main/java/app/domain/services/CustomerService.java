package app.domain.services;

import app.domain.dto.CreateNewCustomer;
import app.domain.model.Customer;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.output.CustomerRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Membership: registering members and keeping their details. */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService implements CustomerUseCase {
    private final CustomerRepositoryPort customerRepositoryPort;

    /** Registers a member, with borrowing privileges switched on. */
    @Override
    public Customer createNewCustomer(CreateNewCustomer createNewCustomer) {
        Customer customer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
        customerRepositoryPort.saveCustomer(customer);
        return customer;
    }

    /** The member with this id, or empty. */
    @Override
    public Optional<Customer> findCustomerById(UUID id) {
        return customerRepositoryPort.getCustomer(id);
    }

    /** The member with exactly this name, or empty. */
    @Override
    public Optional<Customer> findCustomerByName(String customerName) {
        return customerRepositoryPort.getCustomerByName(customerName);
    }

    /** One page of members. */
    @Override
    public Page<Customer> getPaginatedCustomers(Pageable pageable) {
        return customerRepositoryPort.getPaginatedCustomers(pageable);
    }

    /** One page of members matching a free-text query. */
    @Override
    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        return customerRepositoryPort.searchCustomer(query, pageable);
    }

    /** Grants or withdraws borrowing privileges; throws when the member is unknown. */
    @Override
    public void updatePrivileges(UUID id, boolean privileges) {
        Customer customer = customerRepositoryPort.getCustomer(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        customer.setPrivileges(privileges);
        customerRepositoryPort.updatePrivileges(customer);
    }

    /** Overwrites a member's details; throws when the member is unknown. */
    @Override
    public void updateCustomer(Customer customer) {
        if (customerRepositoryPort.getCustomer(customer.getCustomerId()).isEmpty()) {
            throw new EntityNotFoundException("Customer not found with ID: " + customer.getCustomerId());
        }
        customerRepositoryPort.updateCustomer(customer);
    }

    /** Removes a membership. */
    @Override
    public void deleteCustomer(UUID id) {
        customerRepositoryPort.deleteCustomer(id);
    }
}
