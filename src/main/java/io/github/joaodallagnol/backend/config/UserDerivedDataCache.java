package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.gamification.GamificationDashboardResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class UserDerivedDataCache {

    private final CacheManager cacheManager;

    public UserDerivedDataCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterSessionMutation(String userId) {
        afterCommit(() -> {
            Cache dashboard = cacheManager.getCache(CacheNames.GAMIFICATION_DASHBOARD);
            if (dashboard != null) {
                dashboard.evict(userId);
            }
        });
    }

    public GamificationDashboardResponse getDashboard(String userId) {
        Cache dashboard = cacheManager.getCache(CacheNames.GAMIFICATION_DASHBOARD);
        return dashboard == null ? null : dashboard.get(userId, GamificationDashboardResponse.class);
    }

    public void putDashboardAfterRebuild(String userId, GamificationDashboardResponse response) {
        afterCommit(() -> {
            Cache dashboard = cacheManager.getCache(CacheNames.GAMIFICATION_DASHBOARD);
            if (dashboard != null) {
                dashboard.put(userId, response);
            }
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
