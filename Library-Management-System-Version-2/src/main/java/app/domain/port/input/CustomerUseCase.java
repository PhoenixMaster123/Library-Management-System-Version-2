package app.domain.port.input;

import app.domain.dto.CreateNewCustomer;
import app.domain.models.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


public interface CustomerUseCase {
    Customer createNewCustomer(CreateNewCustomer createNewCustomer);
    Optional<Customer> findCustomerByName(String customerName);
    Optional<Customer> findCustomerById(UUID id);
    Page<Customer> getPaginatedCustomers(Pageable pageable);
    Page<Customer> searchCustomer(String query, Pageable pageable);
    void updatePrivileges(UUID id, boolean privileges);
    void updateCustomer(Customer customer);
    void deleteCustomer(UUID id);
}
