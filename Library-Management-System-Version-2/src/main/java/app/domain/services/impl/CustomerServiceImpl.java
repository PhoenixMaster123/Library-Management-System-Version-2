package app.domain.services.impl;

import app.adapters.in.dto.CreateNewCustomer;
import app.domain.models.Customer;
import app.domain.port.CustomerDao;
import app.domain.services.CustomerService;
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
public class CustomerServiceImpl implements CustomerService {
    private final CustomerDao customerDao;

    @Autowired
    public CustomerServiceImpl(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    public Customer createNewCustomer(CreateNewCustomer createNewCustomer) {
        Customer customer = new Customer(null, createNewCustomer.getName(), createNewCustomer.getEmail(), true);
        customerDao.addCustomer(customer);
        return customer;
    }

    public Optional<Customer> findCustomerById(UUID id) {
        return customerDao.getCustomer(id);
    }

    public Optional<Customer> findCustomerByName(String customerName) {
        return customerDao.getCustomerByName(customerName);
    }

    public Page<Customer> getPaginatedCustomers(Pageable pageable) {
        return customerDao.getPaginatedCustomers(pageable);
    }

    public Page<Customer> searchCustomer(String query, Pageable pageable) {
        return customerDao.searchCustomer(query, pageable);
    }

    public void updatePrivileges(UUID id, boolean privileges) {
        Customer customer = findCustomerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + id));

        customer.setPrivileges(privileges);
        customerDao.updatePrivileges(customer);
    }

    public void updateCustomer(Customer customer) {
        if (findCustomerById(customer.getCustomerId()).isEmpty()) {
            throw new EntityNotFoundException("Customer not found with ID: " + customer.getCustomerId());
        }
        customerDao.updateCustomer(customer);
    }

    public void deleteCustomer(UUID id) {
        customerDao.deleteCustomer(id);
    }
}
