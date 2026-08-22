package app.domain.port.output;
import app.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Storage the domain needs for members. */
public interface CustomerRepositoryPort {

    /** Stores a new member. */
    void saveCustomer(Customer customer);

    /** Overwrites the stored member. */
    void updateCustomer(Customer customer);

    /** Writes only the member's borrowing privileges. */
    void updatePrivileges(Customer customer);

    /** Removes the stored member. */
    void deleteCustomer(UUID id);

    /** The stored member with this id, or empty. */
    Optional<Customer> getCustomer(UUID id);

    /** The stored member with exactly this name, or empty. */
    Optional<Customer> getCustomerByName(String name);

    /** One page of stored members. */
    Page<Customer> getPaginatedCustomers(Pageable pageable);

    /** One page of stored members matching a free-text query. */
    Page<Customer> searchCustomer(String query, Pageable pageable);
}
