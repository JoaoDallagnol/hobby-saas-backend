package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.equipment.EquipmentMaintenanceRuleRepository;
import io.github.joaodallagnol.backend.gamification.GoalRepository;
import io.github.joaodallagnol.backend.gamification.HobbyXpRepository;
import io.github.joaodallagnol.backend.gamification.UserBadgeRepository;
import io.github.joaodallagnol.backend.gamification.UserFeaturedBadgeRepository;
import io.github.joaodallagnol.backend.subscription.SubscriptionRepository;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class GamificationTestRepositoryConfig {

    @Bean
    @Primary
    SubscriptionRepository subscriptionRepository() {
        return emptyRepository(SubscriptionRepository.class);
    }

    @Bean
    @Primary
    GoalRepository goalRepository() {
        return emptyRepository(GoalRepository.class);
    }

    @Bean
    @Primary
    HobbyXpRepository hobbyXpRepository() {
        return emptyRepository(HobbyXpRepository.class);
    }

    @Bean
    @Primary
    UserBadgeRepository userBadgeRepository() {
        return emptyRepository(UserBadgeRepository.class);
    }

    @Bean
    @Primary
    UserFeaturedBadgeRepository userFeaturedBadgeRepository() {
        return emptyRepository(UserFeaturedBadgeRepository.class);
    }

    @Bean
    @Primary
    EquipmentMaintenanceRuleRepository equipmentMaintenanceRuleRepository() {
        return emptyRepository(EquipmentMaintenanceRuleRepository.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyRepository(Class<T> repositoryType) {
        return (T) Proxy.newProxyInstance(repositoryType.getClassLoader(), new Class<?>[]{repositoryType},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("toString")) return "Empty" + repositoryType.getSimpleName();
                    if (method.getName().equals("save")) return args[0];
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    if (method.getReturnType().equals(List.class)) return List.of();
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(void.class)) return null;
                    throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                });
    }
}
