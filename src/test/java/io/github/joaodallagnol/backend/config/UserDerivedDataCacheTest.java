package io.github.joaodallagnol.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.gamification.GamificationDashboardResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class UserDerivedDataCacheTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsDashboardOnlyAfterTransactionCommit() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager(CacheNames.GAMIFICATION_DASHBOARD);
        UserDerivedDataCache cache = new UserDerivedDataCache(manager);
        GamificationDashboardResponse response = new GamificationDashboardResponse(
                List.of(), List.of(), null, null
        );
        manager.getCache(CacheNames.GAMIFICATION_DASHBOARD).put("user-1", response);
        TransactionSynchronizationManager.initSynchronization();

        cache.evictAfterSessionMutation("user-1");

        assertThat(cache.getDashboard("user-1")).isSameAs(response);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(cache.getDashboard("user-1")).isNull();
    }
}
