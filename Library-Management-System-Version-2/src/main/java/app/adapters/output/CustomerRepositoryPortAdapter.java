package app.adapters.output;

import app.adapters.output.entity.CustomerEntity;
import app.adapters.output.mapper.EntityMapper;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.model.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

/** Persists members through JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class CustomerRepositoryPortAdapter implements CustomerRepositoryPort {
    private final CustomerRepository customerRepository;

    @Override
    public void saveCustomer(Customer customer) {
        CustomerEntity customerEntity = CustomerEntity.builder()
                .name(customer.getName())
                .email(customer.getEmail())
                .privileges(customer.isPrivileges())
                .build();

        CustomerEntity savedEntity = customerRepository.save(customerEntity);

        customer.setCustomerId(savedEntity.getCustomerId());

    }
    @Override
    public Page<Customer> getPaginatedCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(EntityMapper::toCustomerSummary);
    }

    @Override
    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        return customerRepository.searchByQuery(query.toLowerCase(Locale.ROOT), pageable).map(EntityMapper::toCustomer);
    }

    @Override
    public Optional<Customer> getCustomer(UUID id) {
        return customerRepository.findById(id).map(EntityMapper::toCustomer);
    }

    @Override
    public Optional<Customer> getCustomerByName(String name) {
        return customerRepository.findByName(name).map(EntityMapper::toCustomer);
    }

    @Override
    public void updatePrivileges(Customer customer) {
        customerRepository.findById(customer.getCustomerId())
                .ifPresentOrElse(customerEntity -> {
                    customerEntity.setPrivileges(customer.isPrivileges());
                    customerRepository.save(customerEntity);
                }, () -> {
                    throw new EntityNotFoundException("Customer with ID " + customer.getCustomerId() + " not found");
                });
    }
    @Override
    public void updateCustomer(Customer customer) {
        customerRepository.findById(customer.getCustomerId())
                .ifPresentOrElse(customerEntity -> {
                    customerEntity.setName(customer.getName());
                    customerEntity.setEmail(customer.getEmail());
                    customerEntity.setPrivileges(customer.isPrivileges());
                    customerRepository.save(customerEntity);
                }, () -> {
                    throw new EntityNotFoundException("Customer with ID " + customer.getCustomerId() + " not found");
                });
    }

    @Override
    public void deleteCustomer(UUID id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
        } else {
            throw new RuntimeException("Customer with ID " + id + " not found!");
        }
    }
}
