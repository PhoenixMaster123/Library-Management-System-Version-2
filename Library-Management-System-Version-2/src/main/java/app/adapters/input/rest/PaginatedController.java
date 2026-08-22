package app.adapters.input.rest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/** Shared plumbing for the paginated endpoints: the Link headers and the response body. */
public abstract class PaginatedController {

    /** A page request sorted ascending by the named field. */
    protected PageRequest pageRequest(int page, int size, String sortBy) {
        return PageRequest.of(page, size, Sort.Direction.ASC, sortBy);
    }

    /** self, prev and next Link headers, built from the real mapping rather than hand-made URLs. */
    protected HttpHeaders pageLinks(Page<?> page, IntFunction<Object> pageCall) {
        HttpHeaders headers = new HttpHeaders();
        addLink(headers, "self", pageCall.apply(page.getNumber()));
        if (page.hasPrevious()) {
            addLink(headers, "prev", pageCall.apply(page.getNumber() - 1));
        }
        if (page.hasNext()) {
            addLink(headers, "next", pageCall.apply(page.getNumber() + 1));
        }
        return headers;
    }

    /** Adds one Link header pointing at the given page. */
    private void addLink(HttpHeaders headers, String rel, Object methodOnInvocation) {
        headers.add(rel, "<" + linkTo(methodOnInvocation).toUri() + ">; rel=\"" + rel + "\"");
    }

    /** Insertion-ordered so the JSON key order stays stable. */
    protected Map<String, Object> pageBody(Page<?> page) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", page.getContent());
        body.put("totalPages", page.getTotalPages());
        body.put("currentPage", page.getNumber());
        body.put("totalItems", page.getTotalElements());
        return body;
    }
}
