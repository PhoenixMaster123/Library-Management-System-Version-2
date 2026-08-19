package app.adapters.output.analytics;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** The base URL is configurable so the service can move without a recompile. */
@FeignClient(
        name = "analytics-service",
        url = "${analytics.service.url:http://localhost:9095/api/v1/analytics}"
)
/** HTTP client for Analytics-Service. The base URL is configurable so the service can move. */
public interface AnalyticsFeignClient {

    @GetMapping("/summary")
    SummaryResponse summary();

    @GetMapping("/popular-books")
    List<PopularBookResponse> popularBooks(@RequestParam("limit") int limit);
}
