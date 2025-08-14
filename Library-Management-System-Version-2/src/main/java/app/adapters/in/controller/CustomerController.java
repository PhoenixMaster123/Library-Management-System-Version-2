package app.adapters.in.controller;

import app.adapters.in.dto.CreateNewCustomer;
import app.domain.models.Customer;
import app.domain.services.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    @PostMapping(produces = "application/single-customer-response+json;version=1")
    public ResponseEntity<Customer> createNewCustomer(@Valid @RequestBody CreateNewCustomer newCustomer) {

        Customer customer = customerService.createNewCustomer(newCustomer);

        return ResponseEntity.ok(customer);
    }
    @GetMapping(value = "/{id}", produces = {"application/single-customer-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable UUID id) {
        Optional<Customer> customerOpt = customerService.findCustomerById(id);

        if (customerOpt.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "Customer not found",
                    "customerId", id
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        Map<String, Object> response = Map.of(
                "message", "Customer retrieved successfully",
                "data", customerOpt.get()
        );

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(response);
    }
    @GetMapping(value = "/search", produces = "application/paginated-customers-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        if (id != null) {
            Optional<Customer> customer = customerService.findCustomerById(id);
            return customer.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Customer not found")));
        } else if (name != null && !name.isBlank()) {
            Optional<Customer> customer = customerService.findCustomerByName(name);
            return customer.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Customer with the given name not found")));
        } else if (query != null && !query.isBlank()) {
            int currentPage = page.orElse(0);
            int pageSize = size.orElse(3);
            String sortField = sortBy.orElse("name");

            PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
            Page<Customer> customers = customerService.searchCustomer(query, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("self", "<" + linkTo(methodOn(CustomerController.class)
                    .getCustomer(null, null, query, Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

            if (customers.hasPrevious()) {
                headers.add("prev", "<" + linkTo(methodOn(CustomerController.class)
                        .getCustomer(null, null, query, Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
            }

            if (customers.hasNext()) {
                headers.add("next", "<" + linkTo(methodOn(CustomerController.class)
                        .getCustomer(null, null, query, Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
            }

            if (customers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .headers(headers)
                        .body(Map.of("message", "No customers found for the given query"));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", customers.getContent());
            response.put("totalPages", customers.getTotalPages());
            response.put("currentPage", customers.getNumber());
            response.put("totalItems", customers.getTotalElements());

            return ResponseEntity.ok().headers(headers).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No search criteria provided"));
        }
    }

    @GetMapping(value = "/paginated", produces = "application/paginated-customers-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getAllCustomers(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        int currentPage = page.orElse(0);
        int pageSize = size.orElse(5);
        String sortField = sortBy.orElse("name");

        PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
        Page<Customer> customers = customerService.getPaginatedCustomers(pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("self", "<" + linkTo(methodOn(CustomerController.class)
                .getAllCustomers(Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

        if (customers.hasPrevious()) {
            headers.add("prev", "<" + linkTo(methodOn(CustomerController.class)
                    .getAllCustomers(Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
        }
        if (customers.hasNext()) {
            headers.add("next", "<" + linkTo(methodOn(CustomerController.class)
                    .getAllCustomers(Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
        }

        if (customers.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "There are no customers on this page.",
                    "currentPage", currentPage,
                    "pageSize", pageSize,
                    "sortBy", sortField
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", customers.getContent());
        response.put("totalPages", customers.getTotalPages());
        response.put("currentPage", customers.getNumber());
        response.put("totalItems", customers.getTotalElements());

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @PutMapping(value = "/{id}", produces = "application/single-book-response+json;version=1")
    public ResponseEntity<String> updateCustomer(@NotNull @PathVariable UUID id, @Valid @RequestBody Customer customer) {
        customer.setCustomerId(id);
        customerService.updateCustomer(customer);
        return ResponseEntity.status(HttpStatus.OK).body("Customer updated successfully!");
    }

    @PutMapping(value = "/{id}/privileges", produces = "application/single-book-response+json;version=1")
    public ResponseEntity<String> updateCustomerPrivileges(@NotNull @PathVariable UUID id, @RequestBody(required = false) Boolean privileges) {
        if (privileges == null) {
            return ResponseEntity.badRequest().body("Invalid privileges value");
        }
        customerService.updatePrivileges(id, privileges);
        return ResponseEntity.ok("Customer privileges updated successfully!");
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomer(@NotNull @PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.status(HttpStatus.OK).body("Customer successfully deleted!");
    }
}
