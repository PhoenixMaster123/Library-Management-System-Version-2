package app.domain.services;

import app.domain.model.Book;
import app.domain.port.input.BookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Fills in a book's blurb the first time it is opened, and writes it back.
 *
 * <p>Not called from the borrow or edit paths, which must not wait on an external catalogue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogEnrichmentService {

    private final CatalogDescriptionLookup descriptionLookup;
    private final BookUseCase bookUseCase;

    public Book withDescription(Book book) {
        if (book == null || hasText(book.getDescription()) || !hasText(book.getIsbn())) {
            return book;
        }

        try {
            Optional<String> description = descriptionLookup.forIsbn(book.getIsbn());
            if (description.isEmpty()) {
                return book;
            }

            book.setDescription(description.get());
            bookUseCase.updateBook(book.getBookId(), book);
            return book;
        } catch (Exception e) {
            // A missing blurb is cosmetic; never fail the read over it.
            log.debug("Could not backfill description for ISBN {}: {}", book.getIsbn(), e.getMessage());
            return book;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
