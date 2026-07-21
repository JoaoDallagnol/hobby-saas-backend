package io.github.joaodallagnol.backend.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntitlementServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void missingSubscriptionIsFree() {
        SubscriptionRepository repository = mock(SubscriptionRepository.class);
        when(repository.findById("user-1")).thenReturn(Optional.empty());
        EntitlementService service = service(repository);

        assertThat(service.hasPlus("user-1")).isFalse();
        assertThatThrownBy(() -> service.requirePlus("user-1"))
                .isInstanceOf(PlusPlanRequiredException.class);
    }

    @Test
    void activeUnexpiredSubscriptionGrantsPlusOnlyAfterRolloutCheck() {
        SubscriptionRepository repository = mock(SubscriptionRepository.class);
        FeatureFlagService flags = mock(FeatureFlagService.class);
        when(repository.findById("user-1")).thenReturn(Optional.of(subscription(
                SubscriptionStatus.ACTIVE, OffsetDateTime.parse("2026-08-01T00:00:00Z"))));
        EntitlementService service = new EntitlementService(mock(AuthenticatedUserExtractor.class), repository, flags, CLOCK);

        service.requirePlus("user-1");

        verify(flags).requirePlusFeatures();
        assertThat(service.hasPlus("user-1")).isTrue();
    }

    @Test
    void expiredOrPastDueSubscriptionDoesNotGrantPlus() {
        SubscriptionRepository repository = mock(SubscriptionRepository.class);
        EntitlementService service = service(repository);
        when(repository.findById("expired")).thenReturn(Optional.of(subscription(
                SubscriptionStatus.ACTIVE, OffsetDateTime.parse("2026-07-20T00:00:00Z"))));
        when(repository.findById("past-due")).thenReturn(Optional.of(subscription(
                SubscriptionStatus.PAST_DUE, OffsetDateTime.parse("2026-08-01T00:00:00Z"))));

        assertThat(service.hasPlus("expired")).isFalse();
        assertThat(service.hasPlus("past-due")).isFalse();
    }

    private EntitlementService service(SubscriptionRepository repository) {
        return new EntitlementService(mock(AuthenticatedUserExtractor.class), repository,
                mock(FeatureFlagService.class), CLOCK);
    }

    private Subscription subscription(SubscriptionStatus status, OffsetDateTime periodEnd) {
        Subscription subscription = new Subscription();
        ReflectionTestUtils.setField(subscription, "userId", "user-1");
        ReflectionTestUtils.setField(subscription, "plan", "plus");
        ReflectionTestUtils.setField(subscription, "status", status);
        ReflectionTestUtils.setField(subscription, "currentPeriodEnd", periodEnd);
        return subscription;
    }
}
