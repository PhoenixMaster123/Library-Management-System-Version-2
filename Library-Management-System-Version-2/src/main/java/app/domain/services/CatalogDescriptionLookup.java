package app.domain.services;

import app.domain.dto.CreateNewBook;
import app.domain.port.output.BookCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** One cached blurb lookup per ISBN, misses included. A separate bean so the caching applies. */
@Component
@RequiredArgsConstructor
public class CatalogDescriptionLookup {

    private final BookCatalogPort bookCatalogPort;

    /** The blurb for an ISBN, or empty when there is none. Blank blurbs count as none. */
    @Cacheable(cacheNames = "catalogDescription", key = "#isbn")
    public Optional<String> forIsbn(String isbn) {
        return bookCatalogPort.findByIsbn(isbn)
                .map(CreateNewBook::getDescription)
                .filter(description -> !description.isBlank());
    }
}
