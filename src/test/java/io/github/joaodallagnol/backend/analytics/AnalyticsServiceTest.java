package io.github.joaodallagnol.backend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.gamification.SessionMetricRow;
import io.github.joaodallagnol.backend.gamification.UserBadgeRepository;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {

    @Test
    void comparesEquivalentPeriodsForPlusUser() {
        SessionRecordRepository sessions = mock(SessionRecordRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        FeatureFlagService flags = mock(FeatureFlagService.class);
        when(entitlement.currentUserId()).thenReturn("user-1");
        UUID hobbyId = UUID.randomUUID();
        when(sessions.findGamificationMetricsByUserIdAndPeriod(
                org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                row(hobbyId, 60, "2026-07-20T10:00:00Z"),
                row(hobbyId, 30, "2026-07-21T10:00:00Z"),
                row(hobbyId, 20, "2026-07-18T10:00:00Z")
        ));
        AnalyticsService service = service(sessions, entitlement, flags);

        InsightsResponse response = service.insights(LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-21"));

        assertThat(response.current().sessions()).isEqualTo(2);
        assertThat(response.current().minutes()).isEqualTo(90);
        assertThat(response.previous().sessions()).isEqualTo(1);
        assertThat(response.sessionsChange()).isEqualTo(1);
        verify(flags).requireGamification();
        verify(entitlement).requirePlus("user-1");
    }

    @Test
    void rejectsInvalidOrOversizedPeriodsBeforeQueryingData() {
        EntitlementService entitlement = mock(EntitlementService.class);
        when(entitlement.currentUserId()).thenReturn("user-1");
        AnalyticsService service = service(mock(SessionRecordRepository.class), entitlement,
                mock(FeatureFlagService.class));

        assertThatThrownBy(() -> service.insights(LocalDate.parse("2026-07-21"), LocalDate.parse("2026-07-20")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.insights(LocalDate.parse("2025-01-01"), LocalDate.parse("2026-07-21")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AnalyticsService service(SessionRecordRepository sessions, EntitlementService entitlement,
                                     FeatureFlagService flags) {
        return new AnalyticsService(sessions, entitlement, flags, mock(UserBadgeRepository.class),
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC));
    }

    private SessionMetricRow row(UUID hobbyId, int minutes, String startedAt) {
        return new SessionMetricRow(hobbyId, "Running", "Sports & Movement", 10, 5, minutes,
                OffsetDateTime.parse(startedAt));
    }
}
