package app.adapters.output.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** The catalogue's HTTP client, kept out of the adapter so a test can supply its own. */
@Configuration
public class CatalogClientConfig {

    /** The client the catalogue adapter uses, with timeouts suited to a busy public catalogue. */
    @Bean
    public RestClient catalogRestClient(RestClient.Builder builder,
                                        @Value("${catalog.open-library.url:https://openlibrary.org}") String baseUrl) {
        // Bounded, but generous enough for a busy public catalogue: 3s proved too tight and
        // turned ordinary slowness into "no book found".
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
