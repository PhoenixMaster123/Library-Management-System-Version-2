package app.domain.services;

import app.domain.dto.CreateNewBook;
import app.domain.port.output.BookCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * One cached blurb lookup per ISBN, caching misses too.
 *
 * <p>A separate bean because {@code @Cacheable} does nothing when a class calls its own method.
 */
@Component
@RequiredArgsConstructor
public class CatalogDescriptionLookup {

    private final BookCatalogPort bookCatalogPort;

    @Cacheable(cacheNames = "catalogDescription", key = "#isbn")
    public Optional<String> forIsbn(String isbn) {
        return bookCatalogPort.findByIsbn(isbn)
                .map(CreateNewBook::getDescription)
                .filter(description -> !description.isBlank());
    }
}
