package io.github.joaodallagnol.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableCaching
public class ApplicationCacheConfig {

    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache(CacheNames.HOBBY_CATALOG, 1, Duration.ofHours(24)),
                cache(CacheNames.HOBBY_ATTRIBUTE_TEMPLATES, 500, Duration.ofHours(24)),
                cache(CacheNames.GAMIFICATION_DASHBOARD, 10_000, Duration.ofMinutes(5))
        ));
        return manager;
    }

    private CaffeineCache cache(String name, long maximumSize, Duration ttl) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build());
    }
}
