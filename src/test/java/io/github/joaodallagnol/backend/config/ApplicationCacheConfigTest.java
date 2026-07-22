package io.github.joaodallagnol.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;

class ApplicationCacheConfigTest {

    @Test
    void exposesOnlyTheBoundedApplicationCaches() {
        CacheManager cacheManager = new ApplicationCacheConfig().cacheManager();
        ((SimpleCacheManager) cacheManager).afterPropertiesSet();

        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CacheNames.HOBBY_CATALOG,
                CacheNames.HOBBY_ATTRIBUTE_TEMPLATES,
                CacheNames.GAMIFICATION_DASHBOARD
        );
        assertThat(cacheManager.getCache("unknown")).isNull();
    }
}
