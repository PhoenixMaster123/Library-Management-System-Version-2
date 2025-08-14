package app.infrastructure.config.database;

import app.adapters.in.dto.CreateNewBook;
import app.adapters.in.dto.CreateNewCustomer;
import app.adapters.in.dto.importData.ImportBookDto;
import app.adapters.in.dto.importData.ImportCustomerDto;
import app.adapters.out.H2.repositories.BookRepository;
import app.adapters.out.H2.repositories.CustomerRepository;
import app.domain.services.BookService;
import app.domain.services.CustomerService;
import app.domain.services.TransactionService;
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
    private final BookService bookService;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final Gson gson;
    private final ModelMapper mapper;

    public DatabaseSeeder(BookService bookService, CustomerService customerService, TransactionService transactionService, BookRepository bookRepository, CustomerRepository customerRepository, Gson gson, ModelMapper mapper) {
        this.bookService = bookService;
        this.customerService = customerService;
        this.transactionService = transactionService;
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
                    transactionService.borrowBookWithDates(customerId, bookId, borrowDate);
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
                        bookService.createNewBook(create);
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
                        customerService.createNewCustomer(dto);
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

            app.adapters.in.dto.importData.ImportTransactionDto[] txs = gson.fromJson(json, app.adapters.in.dto.importData.ImportTransactionDto[].class);
            if (txs == null || txs.length == 0) {
                System.out.println("No transactions to import.");
                return true;
            }

            for (app.adapters.in.dto.importData.ImportTransactionDto t : txs) {
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
                    transactionService.borrowBookWithDates(customerId, bookId, borrowDate);
                    System.out.println("Imported borrow: book=" + isbn + ", customer=" + name + ", on=" + borrowDate);

                    if (returnStr != null && !returnStr.isBlank()) {
                        LocalDate returnDate = LocalDate.parse(returnStr);
                        transactionService.returnBookWithDates(bookId, returnDate);
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
