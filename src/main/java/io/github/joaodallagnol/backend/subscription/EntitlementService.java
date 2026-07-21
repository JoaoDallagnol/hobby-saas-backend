package io.github.joaodallagnol.backend.subscription;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class EntitlementService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final SubscriptionRepository subscriptionRepository;
    private final FeatureFlagService featureFlagService;
    private final Clock clock;

    public EntitlementService(AuthenticatedUserExtractor authenticatedUserExtractor,
                              SubscriptionRepository subscriptionRepository,
                              FeatureFlagService featureFlagService,
                              Clock clock) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.subscriptionRepository = subscriptionRepository;
        this.featureFlagService = featureFlagService;
        this.clock = clock;
    }

    public CurrentPlanResponse currentPlan() {
        String userId = currentUserId();
        return subscriptionRepository.findById(userId)
                .filter(this::isActivePlus)
                .map(subscription -> new CurrentPlanResponse("plus", true, subscription.getCurrentPeriodEnd()))
                .orElseGet(() -> new CurrentPlanResponse("free", false, null));
    }

    public boolean hasPlus(String userId) {
        return subscriptionRepository.findById(userId).filter(this::isActivePlus).isPresent();
    }

    public void requirePlus(String userId) {
        featureFlagService.requirePlusFeatures();
        if (!hasPlus(userId)) {
            throw new PlusPlanRequiredException();
        }
    }

    public String currentUserId() {
        return authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication()).id();
    }

    private boolean isActivePlus(Subscription subscription) {
        return "plus".equals(subscription.getPlan())
                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                && (subscription.getCurrentPeriodEnd() == null
                    || subscription.getCurrentPeriodEnd().isAfter(OffsetDateTime.now(clock)));
    }
}
