package app.domain.port.output;
import app.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort {

    void saveCustomer(Customer customer);
    void updateCustomer(Customer customer);
    void updatePrivileges(Customer customer);
    void deleteCustomer(UUID id);
    Optional<Customer> getCustomer(UUID id);
    Optional<Customer> getCustomerByName(String name);
    Page<Customer> getPaginatedCustomers(Pageable pageable);
    Page<Customer> searchCustomer(String query, Pageable pageable);
}
