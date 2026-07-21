package io.github.joaodallagnol.backend.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GamificationServiceTest {

    @Test
    void calculatesXpRecordsStreakChallengeAndAwardsServerBadges() {
        SessionRecordRepository sessions = mock(SessionRecordRepository.class);
        HobbyXpRepository xpRepository = mock(HobbyXpRepository.class);
        UserBadgeRepository badgeRepository = mock(UserBadgeRepository.class);
        HobbyRepository hobbies = mock(HobbyRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        FeatureFlagService flags = mock(FeatureFlagService.class);
        UUID hobbyId = UUID.fromString("93dfa1cf-759e-43fc-af34-060446cb7601");
        when(entitlement.currentUserId()).thenReturn("user-1");
        when(badgeRepository.findAllByUserIdOrderByEarnedAtDesc("user-1")).thenReturn(List.of());
        when(badgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(xpRepository.save(any(HobbyXp.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessions.findGamificationMetricsByUserId("user-1")).thenReturn(List.of(
                row(hobbyId, 60, "2026-07-01T10:00:00Z"),
                row(hobbyId, 45, "2026-07-02T10:00:00Z"),
                row(hobbyId, 30, "2026-07-03T10:00:00Z"),
                row(hobbyId, 20, "2026-07-04T10:00:00Z"),
                row(hobbyId, 15, "2026-07-05T10:00:00Z")
        ));
        GamificationService service = new GamificationService(sessions, xpRepository, badgeRepository, hobbies,
                entitlement, flags, Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        GamificationDashboardResponse response = service.dashboard();

        assertThat(response.hobbies()).singleElement().satisfies(progress -> {
            assertThat(progress.xp()).isEqualTo(84);
            assertThat(progress.level()).isEqualTo(1);
            assertThat(progress.nextLevelXp()).isEqualTo(100);
        });
        assertThat(response.records().longestSessionMinutes()).isEqualTo(60);
        assertThat(response.records().bestStreakDays()).isEqualTo(5);
        assertThat(response.monthlyChallenge().progress()).isEqualTo(5);
        assertThat(response.badges()).extracting(BadgeResponse::key)
                .contains("first_session", "sessions_5");
        verify(flags).requireGamification();
        verify(xpRepository).deleteAllByUserId("user-1");
    }

    @Test
    void emptyHistoryProducesStableZeroDashboard() {
        SessionRecordRepository sessions = mock(SessionRecordRepository.class);
        HobbyXpRepository xpRepository = mock(HobbyXpRepository.class);
        UserBadgeRepository badges = mock(UserBadgeRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        when(entitlement.currentUserId()).thenReturn("user-1");
        when(sessions.findGamificationMetricsByUserId("user-1")).thenReturn(List.of());
        when(badges.findAllByUserIdOrderByEarnedAtDesc("user-1")).thenReturn(List.of());
        GamificationService service = new GamificationService(sessions, xpRepository, badges,
                mock(HobbyRepository.class), entitlement, mock(FeatureFlagService.class),
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));

        GamificationDashboardResponse response = service.dashboard();

        assertThat(response.hobbies()).isEmpty();
        assertThat(response.badges()).isEmpty();
        assertThat(response.records().bestStreakDays()).isZero();
        assertThat(response.monthlyChallenge().achieved()).isFalse();
    }

    private SessionMetricRow row(UUID hobbyId, int minutes, String startedAt) {
        return new SessionMetricRow(hobbyId, "Running", "Sports & Movement", 10, 5, minutes,
                OffsetDateTime.parse(startedAt));
    }
}
