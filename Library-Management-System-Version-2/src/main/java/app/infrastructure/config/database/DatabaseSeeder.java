package app.infrastructure.config.database;

import app.domain.dto.CreateNewBook;
import app.domain.dto.CreateNewCustomer;
import app.domain.dto.importData.ImportBookDto;
import app.domain.dto.importData.ImportCustomerDto;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.domain.dto.importData.ImportTransactionDto;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.input.TransactionUseCase;
import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {
    private final BookUseCase bookUseCase;
    private final CustomerUseCase customerUseCase;
    private final TransactionUseCase transactionUseCase;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final Gson gson;
    private final ModelMapper mapper;

    public DatabaseSeeder(BookUseCase bookUseCase, CustomerUseCase customerUseCase, TransactionUseCase transactionUseCase, BookRepository bookRepository, CustomerRepository customerRepository, Gson gson, ModelMapper mapper) {
        this.bookUseCase = bookUseCase;
        this.customerUseCase = customerUseCase;
        this.transactionUseCase = transactionUseCase;
        this.bookRepository = bookRepository;
        this.customerRepository = customerRepository;
        this.gson = gson;
        this.mapper = mapper;
    }

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
                    System.out.println("[Seeder Fallback] Borrowed book: " + bookId + " by customer: " + customerId + " on " + borrowDate);
                } catch (Exception e) {
                    System.out.println("[Seeder Fallback] Skipping transaction for customer " + customerId + " and book " + bookId + " - " + e.getMessage());
                }
            }
        }
    }

    private List<UUID> importBooksFromJson(){
        List<UUID> bookIds = new ArrayList<>();
        try {
            Path path = new ClassPathResource("files/json/books.json").getFile().toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String json = String.join("", lines);

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
                        System.out.println("Seeded book: " + create.getTitle());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Skipping book: " + create.getTitle() + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to read books JSON via readAllLines: " + e.getMessage());
        }
        return bookIds;
    }

    private List<UUID> importCustomersFromJson() {
        List<UUID> customerIds = new ArrayList<>();
        try {
            Path path = new ClassPathResource("files/json/customers.json").getFile().toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String json = String.join("", lines);

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
                        System.out.println("Seeded customer: " + dto.getName());
                    } catch (Exception e) {
                        System.out.println("Skipping customer: " + dto.getName() + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to read customers JSON via readAllLines: " + e.getMessage());
        }
        return customerIds;
    }

    private boolean importTransactionsFromJson() {
        try {
            Path path = new ClassPathResource("files/json/transactions.json").getFile().toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String json = String.join("", lines);

            ImportTransactionDto[] txs = gson.fromJson(json, ImportTransactionDto[].class);
            if (txs == null || txs.length == 0) {
                System.out.println("No transactions to import.");
                return true;
            }

            for (ImportTransactionDto t : txs) {
                String name = t.getCustomerName();
                String isbn = t.getBookIsbn();
                String borrowStr = t.getBorrowDate();
                String returnStr = t.getReturnDate();
                try {
                    if (name == null || isbn == null || borrowStr == null) {
                        System.out.println("Skipping transaction due to missing required fields (customerName/bookIsbn/borrowDate).");
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
                    System.out.println("Imported borrow: book=" + isbn + ", customer=" + name + ", on=" + borrowDate);

                    if (returnStr != null && !returnStr.isBlank()) {
                        LocalDate returnDate = LocalDate.parse(returnStr);
                        transactionUseCase.returnBookWithDates(bookId, returnDate);
                        System.out.println("Imported return: book=" + isbn + ", on=" + returnDate);
                    }
                } catch (Exception ex) {
                    System.out.println("Skipping transaction (customer=" + name + ", isbn=" + isbn + ") - " + ex.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("Transactions JSON not found or failed to read: " + e.getMessage());
            return false;
        }
    }
}
