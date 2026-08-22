package app.infrastructure.config.database;

import app.adapters.output.repositories.BookRepository;
import app.domain.model.CatalogCandidate;
import app.domain.port.output.BookCatalogPort;
import app.domain.services.CatalogImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stocks an empty library from Open Library after start-up. A stocked one is left alone. */
@Component
@Profile("!dev")
@ConditionalOnProperty(name = "library.catalog.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CatalogSeeder {

    private final BookRepository bookRepository;
    private final BookCatalogPort bookCatalogPort;
    private final CatalogImportService catalogImportService;

    /** Broad enough to fill a general library, and each one is a single search. */
    @Value("${library.catalog.seed.subjects:"
            + "fiction,science fiction,fantasy,history,philosophy,poetry,biography,"
            + "mystery,psychology,mathematics,travel,art}")
    private List<String> subjects;

    @Value("${library.catalog.seed.per-subject:40}")
    private int perSubject;

    /** Fills an empty catalogue once the app is up, one search per subject, off the main thread. */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void seedIfEmpty() {
        if (bookRepository.count() > 0) {
            log.debug("Catalogue already stocked; skipping the Open Library seed.");
            return;
        }

        log.info("Empty catalogue: stocking it from Open Library across {} subject(s).", subjects.size());

        // Keyed by ISBN because the same book turns up under several subjects, and a duplicate
        // would only be rejected later by the title check.
        Map<String, CatalogCandidate> candidates = new LinkedHashMap<>();
        for (String subject : subjects) {
            try {
                bookCatalogPort.search(subject.trim(), 0, perSubject).results()
                        .forEach(candidate -> candidates.putIfAbsent(candidate.isbn(), candidate));
            } catch (Exception e) {
                log.warn("Could not read subject '{}' from Open Library: {}", subject, e.getMessage());
            }
        }

        if (candidates.isEmpty()) {
            log.warn("Open Library returned nothing; the catalogue is still empty. "
                    + "Books can be added from the Discover page once it is reachable.");
            return;
        }

        CatalogImportService.ImportSummary summary = catalogImportService.importCandidates(candidates.values());
        log.info("Stocked {} book(s) from Open Library, skipped {}.",
                summary.imported().size(), summary.skipped().size());
    }
}
