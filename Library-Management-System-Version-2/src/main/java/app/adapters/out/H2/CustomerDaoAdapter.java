package app.adapters.out.H2;

import app.adapters.out.H2.entity.CustomerEntity;
import app.adapters.out.H2.repositories.CustomerRepository;
import app.domain.models.Transaction;
import app.domain.port.CustomerDao;
import app.domain.models.Customer;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerDaoAdapter implements CustomerDao {
    private final CustomerRepository customerRepository;

    public CustomerDaoAdapter(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void addCustomer(Customer customer) {
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
        return customerRepository.findAll(pageable).map(customerEntity -> new Customer(
                customerEntity.getCustomerId(),
                customerEntity.getName(),
                customerEntity.getEmail(),
                customerEntity.isPrivileges()
        ));
    }
    @Override
    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        String lowerQuery = query.toLowerCase();

        Page<CustomerEntity> customerEntity =
                customerRepository.searchByQuery(lowerQuery, pageable);

        return customerEntity.map(this::mapCustomerEntityToCustomer);
    }

    @Override
    public Optional<Customer> getCustomer(UUID id) {
        return customerRepository.findById(id)
                .map(this::mapCustomerEntityToCustomer);
    }
    @Override
    public Optional<Customer> getCustomerByName(String name) {
        return customerRepository.findByName(name)
                .map(this::mapCustomerEntityToCustomer);
    }
    private Customer mapCustomerEntityToCustomer(CustomerEntity customerEntity) {
        Customer customer = new Customer(
                customerEntity.getCustomerId(),
                customerEntity.getName(),
                customerEntity.getEmail(),
                customerEntity.isPrivileges()
        );

        customerEntity.getTransactions().forEach(transactionEntity -> {
            Transaction transaction = new Transaction(
                    transactionEntity.getTransactionId(),
                    transactionEntity.getBorrowDate(),
                    transactionEntity.getReturnDate(),
                    transactionEntity.getDueDate()
            );

            transaction.setCustomerId(customerEntity.getCustomerId());
            transaction.setBookId(transactionEntity.getBook() != null ? transactionEntity.getBook().getBookId() : null);

            customer.getTransactions().add(transaction);
        });

        return customer;
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
