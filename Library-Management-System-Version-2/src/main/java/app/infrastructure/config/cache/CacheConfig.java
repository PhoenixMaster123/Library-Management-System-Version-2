package app.infrastructure.config.cache;

import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class CacheConfig {
    @Bean
    public Filter shallowEtagFilter() {
        return new ShallowEtagHeaderFilter();
    }
}
