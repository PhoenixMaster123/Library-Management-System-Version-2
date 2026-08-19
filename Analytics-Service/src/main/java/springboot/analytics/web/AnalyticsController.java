package springboot.analytics.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springboot.analytics.service.LoanStatisticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final LoanStatisticsService statistics;

    @GetMapping("/summary")
    public ResponseEntity<LoanStatisticsService.Summary> summary() {
        return ResponseEntity.ok(statistics.summary());
    }

    @GetMapping("/popular-books")
    public ResponseEntity<List<Map<String, Object>>> popularBooks(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> books = statistics.mostBorrowed(limit).stream()
                .map(stat -> Map.<String, Object>of(
                        "bookId", stat.getBookId(),
                        "title", stat.getTitle(),
                        "isbn", stat.getIsbn() == null ? "" : stat.getIsbn(),
                        "timesBorrowed", stat.getTimesBorrowed(),
                        "timesReturned", stat.getTimesReturned(),
                        "currentlyOut", stat.getCurrentlyOut()))
                .toList();

        return ResponseEntity.ok(books);
    }
}
