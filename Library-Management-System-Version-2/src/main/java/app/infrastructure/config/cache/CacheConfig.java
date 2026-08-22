package app.infrastructure.config.cache;

import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** Enables caching and names the caches. */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    /** Adds ETags to responses, so an unchanged body comes back as a 304. */
    @Bean
    public Filter shallowEtagFilter() {
        return new ShallowEtagHeaderFilter();
    }

    /** Logs cache failures and carries on: a cache is an optimisation, never a precondition. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            /** Serves the call uncached when the cache cannot be read. */
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' unreadable, serving uncached: {}", cache.getName(), exception.getMessage());
            }

            /** Carries on when a result cannot be written to the cache. */
            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache '{}' unwritable: {}", cache.getName(), exception.getMessage());
            }

            /** Carries on when an entry cannot be evicted. */
            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' eviction failed: {}", cache.getName(), exception.getMessage());
            }

            /** Carries on when the cache cannot be cleared. */
            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache '{}' clear failed: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
