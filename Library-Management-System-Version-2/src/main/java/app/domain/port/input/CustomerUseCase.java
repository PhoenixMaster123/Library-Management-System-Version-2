package app.domain.port.input;

import app.domain.dto.CreateNewCustomer;
import app.domain.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


/** What the application can be asked to do with members. */
public interface CustomerUseCase {
    /** Registers a member and returns them with their assigned id. */
    Customer createNewCustomer(CreateNewCustomer createNewCustomer);

    /** The member with exactly this name, or empty. */
    Optional<Customer> findCustomerByName(String customerName);

    /** The member with this id, or empty. */
    Optional<Customer> findCustomerById(UUID id);

    /** One page of members. */
    Page<Customer> getPaginatedCustomers(Pageable pageable);

    /** One page of members matching a free-text query. */
    Page<Customer> searchCustomer(String query, Pageable pageable);

    /** Grants or withdraws a member's borrowing privileges. */
    void updatePrivileges(UUID id, boolean privileges);

    /** Overwrites a member's details. */
    void updateCustomer(Customer customer);

    /** Removes a membership. */
    void deleteCustomer(UUID id);
}
