package app.infrastructure.config.database;

import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.dto.CreateNewBook;
import app.domain.dto.CreateNewCustomer;
import app.domain.dto.importdata.ImportBookDto;
import app.domain.dto.importdata.ImportCustomerDto;
import app.domain.dto.importdata.ImportTransactionDto;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.input.TransactionUseCase;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The dev fixture: books, members and loans read from resources/files/json. Dev profile only. */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {
    private final BookUseCase bookUseCase;
    private final CustomerUseCase customerUseCase;
    private final TransactionUseCase transactionUseCase;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final Gson gson;
    private final ModelMapper mapper;

    /** Loads the fixture at start-up, unless the library already holds books. */
    @Override
    public void run(String... args){
        List<UUID> customerIds;
        List<UUID> bookIds;

        bookIds = importBooksFromJson();
        customerIds = importCustomersFromJson();
        boolean imported = importTransactionsFromJson();

        if (!imported) {
            int max = Math.min(3, Math.min(customerIds.size(), bookIds.size()));
            for (int i = 0; i < max; i++) {
                UUID customerId = customerIds.get(i);
                UUID bookId = bookIds.get(i);
                try {
                    LocalDate borrowDate = LocalDate.now();
                    transactionUseCase.borrowBookWithDates(customerId, bookId, borrowDate);
                    log.info("[Seeder Fallback] Borrowed book: {} by customer: {} on {}",
                            bookId, customerId, borrowDate);
                } catch (Exception e) {
                    log.warn("[Seeder Fallback] Skipping transaction for customer {} and book {} - {}",
                            customerId, bookId, e.getMessage());
                }
            }
        }
    }

    /** Adds the fixture's books and returns their ids. */
    private List<UUID> importBooksFromJson(){
        List<UUID> bookIds = new ArrayList<>();
        try {
            String json = readClasspathJson("files/json/books.json");

            ImportBookDto[] importBooks = gson.fromJson(json, ImportBookDto[].class);
            if (importBooks != null) {
                for (ImportBookDto ib : importBooks) {
                    CreateNewBook create = mapper.map(ib, CreateNewBook.class);
                    try {
                        bookUseCase.createNewBook(create);
                        UUID bookId = bookRepository.findBooksByIsbn(create.getIsbn())
                                .orElseThrow(() -> new IllegalStateException("Book not found"))
                                .getBookId();
                        bookIds.add(bookId);
                        log.info("Seeded book: {}", create.getTitle());
                    } catch (IllegalArgumentException e) {
                        log.warn("Skipping book: {} - {}", create.getTitle(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read books JSON: {}", e.getMessage());
        }
        return bookIds;
    }

    /** Adds the fixture's members and returns their ids. */
    private List<UUID> importCustomersFromJson() {
        List<UUID> customerIds = new ArrayList<>();
        try {
            String json = readClasspathJson("files/json/customers.json");

            ImportCustomerDto[] importCustomers = gson.fromJson(json, ImportCustomerDto[].class);
            if (importCustomers != null) {
                for (ImportCustomerDto ic : importCustomers) {
                    CreateNewCustomer dto = mapper.map(ic, CreateNewCustomer.class);
                    try {
                        customerUseCase.createNewCustomer(dto);
                        UUID customerId = customerRepository.findByName(dto.getName())
                                .orElseThrow(() -> new IllegalStateException("Customer not found"))
                                .getCustomerId();
                        customerIds.add(customerId);
                        log.info("Seeded customer: {}", dto.getName());
                    } catch (Exception e) {
                        log.warn("Skipping customer: {} - {}", dto.getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read customers JSON: {}", e.getMessage());
        }
        return customerIds;
    }

    /** Replays the fixture's borrows and returns; false when nothing could be read. */
    private boolean importTransactionsFromJson() {
        try {
            String json = readClasspathJson("files/json/transactions.json");

            ImportTransactionDto[] txs = gson.fromJson(json, ImportTransactionDto[].class);
            if (txs == null || txs.length == 0) {
                log.info("No transactions to import.");
                return true;
            }

            for (ImportTransactionDto t : txs) {
                String name = t.getCustomerName();
                String isbn = t.getBookIsbn();
                String borrowStr = t.getBorrowDate();
                String returnStr = t.getReturnDate();
                try {
                    if (name == null || isbn == null || borrowStr == null) {
                        log.warn("Skipping transaction due to missing required fields "
                                + "(customerName/bookIsbn/borrowDate).");
                        continue;
                    }
                    UUID customerId = customerRepository.findByName(name)
                            .orElseThrow(() -> new IllegalStateException("Customer not found: " + name))
                            .getCustomerId();
                    UUID bookId = bookRepository.findBooksByIsbn(isbn)
                            .orElseThrow(() -> new IllegalStateException("Book not found by ISBN: " + isbn))
                            .getBookId();

                    LocalDate borrowDate = LocalDate.parse(borrowStr);
                    transactionUseCase.borrowBookWithDates(customerId, bookId, borrowDate);
                    log.info("Imported borrow: book={}, customer={}, on={}", isbn, name, borrowDate);

                    if (returnStr != null && !returnStr.isBlank()) {
                        LocalDate returnDate = LocalDate.parse(returnStr);
                        transactionUseCase.returnBookWithDates(bookId, returnDate);
                        log.info("Imported return: book={}, on={}", isbn, returnDate);
                    }
                } catch (Exception ex) {
                    log.warn("Skipping transaction (customer={}, isbn={}) - {}", name, isbn, ex.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Transactions JSON not found or failed to read: {}", e.getMessage());
            return false;
        }
    }

    /** Reads a seed file as a classpath stream, which unlike getFile() also works inside the jar. */
    private String readClasspathJson(String location) throws IOException {
        try (InputStream stream = new ClassPathResource(location).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
