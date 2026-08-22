package app.adapters.output.analytics;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** HTTP client for Analytics-Service. The base URL is configurable so the service can move. */
@FeignClient(
        name = "analytics-service",
        url = "${analytics.service.url:http://localhost:9095/api/v1/analytics}"
)
public interface AnalyticsFeignClient {

    /** The library-wide totals. */
    @GetMapping("/summary")
    SummaryResponse summary();

    /** The most borrowed books, at most limit of them. */
    @GetMapping("/popular-books")
    List<PopularBookResponse> popularBooks(@RequestParam("limit") int limit);
}
