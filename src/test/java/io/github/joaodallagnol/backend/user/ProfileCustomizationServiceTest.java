package io.github.joaodallagnol.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.gamification.UserBadge;
import io.github.joaodallagnol.backend.gamification.UserBadgeRepository;
import io.github.joaodallagnol.backend.gamification.UserFeaturedBadge;
import io.github.joaodallagnol.backend.gamification.UserFeaturedBadgeRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProfileCustomizationServiceTest {

    @Test
    void plusUserCanSelectThemeAndOnlyOwnedBadges() {
        ProductUserRepository users = mock(ProductUserRepository.class);
        UserBadgeRepository badges = mock(UserBadgeRepository.class);
        UserFeaturedBadgeRepository featured = mock(UserFeaturedBadgeRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        ProductUser user = new ProductUser("user-1", "user@example.com", "User", true, null,
                OffsetDateTime.parse("2026-07-01T00:00:00Z"));
        UserBadge badge = new UserBadge("user-1", "first_session", null,
                OffsetDateTime.parse("2026-07-20T10:00:00Z"));
        when(entitlement.currentUserId()).thenReturn("user-1");
        when(entitlement.hasPlus("user-1")).thenReturn(true);
        when(users.findById("user-1")).thenReturn(Optional.of(user));
        when(badges.findAllById(List.of(badge.getId()))).thenReturn(List.of(badge));
        when(featured.findAllByUserIdOrderByPositionAsc("user-1")).thenReturn(List.of());
        ProfileCustomizationService service = new ProfileCustomizationService(users, badges, featured, entitlement);

        ProfileCustomizationResponse response = service.update(
                new ProfileCustomizationRequest("midnight", List.of(badge.getId())));

        assertThat(response.theme()).isEqualTo("midnight");
        assertThat(response.supporterBadge()).isTrue();
        verify(entitlement).requirePlus("user-1");
        verify(featured).deleteAllByUserId("user-1");
        verify(featured).save(any(UserFeaturedBadge.class));
    }

    @Test
    void rejectsBadgeOwnedByAnotherUser() {
        UserBadgeRepository badges = mock(UserBadgeRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        when(entitlement.currentUserId()).thenReturn("user-1");
        UserBadge foreign = new UserBadge("user-2", "first_session", null, OffsetDateTime.now());
        when(badges.findAllById(List.of(foreign.getId()))).thenReturn(List.of(foreign));
        ProfileCustomizationService service = new ProfileCustomizationService(mock(ProductUserRepository.class), badges,
                mock(UserFeaturedBadgeRepository.class), entitlement);

        assertThatThrownBy(() -> service.update(
                new ProfileCustomizationRequest("forest", List.of(foreign.getId()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not belong");
    }
}
