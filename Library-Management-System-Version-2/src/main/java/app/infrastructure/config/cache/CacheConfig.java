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

    @Bean
    public Filter shallowEtagFilter() {
        return new ShallowEtagHeaderFilter();
    }

    /**
     * A cache is an optimisation, never a precondition. Without this, an unreachable cache server
     * turns every cached call into a 500 - which is exactly what happened when the Redis starter
     * on the classpath quietly became the cache manager with no Redis running.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' unreadable, serving uncached: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache '{}' unwritable: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache '{}' eviction failed: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache '{}' clear failed: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
