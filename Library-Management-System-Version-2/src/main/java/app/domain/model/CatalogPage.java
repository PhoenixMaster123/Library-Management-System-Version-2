package app.domain.model;

import java.util.List;

/**
 * One page of external-catalogue hits.
 *
 * <p>{@code totalItems} is what the catalogue reports for the whole query, not the size of
 * {@code results} - it is what lets the picker say "1,174 found" and offer page 47.
 */
public record CatalogPage(List<CatalogCandidate> results, int totalItems) {

    public static CatalogPage empty() {
        return new CatalogPage(List.of(), 0);
    }
}
