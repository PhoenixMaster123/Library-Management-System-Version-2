package app.domain.model;

import java.util.List;

/** One page of external-catalogue hits. totalItems counts the whole query, not this page. */
public record CatalogPage(List<CatalogCandidate> results, int totalItems) {

    /** No hits at all - what a failed or empty search returns. */
    public static CatalogPage empty() {
        return new CatalogPage(List.of(), 0);
    }
}
