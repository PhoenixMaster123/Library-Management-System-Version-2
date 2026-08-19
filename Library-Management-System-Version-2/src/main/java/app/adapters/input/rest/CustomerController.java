package app.adapters.input.rest;

import app.domain.dto.CreateNewCustomer;
import app.domain.model.Customer;
import app.domain.port.input.CustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** Member records: reading, searching and updating them. Administrators only. */
@RestController
@RequestMapping("/customers")
@Tag(name = "Customer Controller", description = "Endpoints for managing customers")
@RequiredArgsConstructor
public class CustomerController extends PaginatedController {
    private final CustomerUseCase customerUseCase;

    /**
     * No BindingResult: letting {@code @Valid} throw means GlobalExceptionHandler answers with the
     * field errors, and this method can promise a concrete Customer instead of a wildcard.
     */
    @PostMapping(produces = {"application/single-customer-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Create a new customer")
    public ResponseEntity<Customer> createNewCustomer(@Valid @RequestBody CreateNewCustomer newCustomer) {
        return ResponseEntity.ok(customerUseCase.createNewCustomer(newCustomer));
    }

    @GetMapping(value = "/{id}",
            produces = {"application/single-customer-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get a single customer")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable UUID id) {
        Optional<Customer> customerOpt = customerUseCase.findCustomerById(id);

        if (customerOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Customer not found", "customerId", id));
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(Map.of("message", "Customer retrieved successfully", "data", customerOpt.get()));
    }

    @GetMapping(value = "/search",
            produces = {"application/paginated-customers-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Search for a customer by name or ID or query")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        if (id != null) {
            Optional<Customer> customer = customerUseCase.findCustomerById(id);
            return customer.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Customer not found")));
        } else if (name != null && !name.isBlank()) {
            Optional<Customer> customer = customerUseCase.findCustomerByName(name);
            return customer.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Customer with the given name not found")));
        } else if (query != null && !query.isBlank()) {
            PageRequest pageable = pageRequest(page, size, sortBy);
            Page<Customer> customers = customerUseCase.searchCustomer(query, pageable);

            HttpHeaders headers = pageLinks(customers, p -> methodOn(CustomerController.class)
                    .getCustomer(null, null, query, p, size, sortBy));

            if (customers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .headers(headers)
                        .body(Map.of("message", "No customers found for the given query"));
            }

            return ResponseEntity.ok().headers(headers).body(pageBody(customers));
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No search criteria provided"));
        }
    }

    @GetMapping(value = "/paginated",
            produces = {"application/paginated-customers-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get all customers")
    public ResponseEntity<Map<String, Object>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        PageRequest pageable = pageRequest(page, size, sortBy);
        Page<Customer> customers = customerUseCase.getPaginatedCustomers(pageable);

        HttpHeaders headers = pageLinks(customers, p -> methodOn(CustomerController.class)
                .getAllCustomers(p, size, sortBy));

        if (customers.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "There are no customers on this page.",
                    "currentPage", page,
                    "pageSize", size,
                    "sortBy", sortBy
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }

        return ResponseEntity.ok().headers(headers).body(pageBody(customers));
    }

    @PutMapping(value = "/{id}",
            produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Update a customer")
    public ResponseEntity<String> updateCustomer(@PathVariable UUID id,
                                                 @Valid @RequestBody Customer customer,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Invalid customer data");
        }
        customer.setCustomerId(id);
        customerUseCase.updateCustomer(customer);
        return ResponseEntity.status(HttpStatus.OK).body("Customer updated successfully!");
    }

    @PutMapping(value = "/{id}/privileges",
            produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Update a customer privileges")
    public ResponseEntity<String> updateCustomerPrivileges(@PathVariable UUID id,
                                                           @RequestBody(required = false) Boolean privileges) {
        if (privileges == null) {
            return ResponseEntity.badRequest().body("Invalid privileges value");
        }
        customerUseCase.updatePrivileges(id, privileges);
        return ResponseEntity.ok("Customer privileges updated successfully!");
    }

    @DeleteMapping(value = "/{id}",
            produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Delete a customer")
    public ResponseEntity<String> deleteCustomer(@PathVariable UUID id) {
        customerUseCase.deleteCustomer(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Customer successfully deleted!");
    }
}
