package io.github.joaodallagnol.backend.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class GoalServiceTest {

    private final GoalRepository goalRepository = mock(GoalRepository.class);
    private final SessionRecordRepository sessionRepository = mock(SessionRecordRepository.class);
    private final HobbyRepository hobbyRepository = mock(HobbyRepository.class);
    private final UserHobbyRepository userHobbyRepository = mock(UserHobbyRepository.class);
    private final EntitlementService entitlementService = mock(EntitlementService.class);
    private final FeatureFlagService featureFlags = mock(FeatureFlagService.class);
    private final UUID hobbyId = UUID.fromString("93dfa1cf-759e-43fc-af34-060446cb7601");
    private final Hobby hobby = BeanUtils.instantiateClass(Hobby.class);
    private final GoalService service = new GoalService(goalRepository, sessionRepository, hobbyRepository,
            userHobbyRepository, entitlementService, featureFlags,
            Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hobby, "id", hobbyId);
        ReflectionTestUtils.setField(hobby, "name", "Running");
        when(entitlementService.currentUserId()).thenReturn("user-1");
        when(userHobbyRepository.existsByIdUserIdAndIdHobbyId("user-1", hobbyId)).thenReturn(true);
        when(hobbyRepository.findById(hobbyId)).thenReturn(Optional.of(hobby));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void freeUserCanCreateOneWeeklyGoalPerHobbyWithDerivedProgress() {
        when(goalRepository.countOverlapping(org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq(hobbyId), org.mockito.ArgumentMatchers.eq(GoalStatus.ACTIVE),
                org.mockito.ArgumentMatchers.eq(GoalCadence.WEEKLY), any(), any())).thenReturn(0L);
        when(sessionRepository.countByUserIdAndHobbyIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.eq(hobbyId), any(), any()))
                .thenReturn(2L);

        GoalResponse response = service.create(new GoalRequest(hobbyId, "Treinar", "sessions", 4,
                "weekly", null, null));

        assertThat(response.startDate()).isEqualTo(LocalDate.parse("2026-07-20"));
        assertThat(response.endDate()).isEqualTo(LocalDate.parse("2026-07-26"));
        assertThat(response.progress()).isEqualTo(2);
        assertThat(response.progressPercent()).isEqualTo(50);
        assertThat(response.advanced()).isFalse();
        verify(entitlementService, never()).requirePlus("user-1");
    }

    @Test
    void secondWeeklyGoalAndCustomGoalRequirePlus() {
        when(goalRepository.countOverlapping(org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq(hobbyId), org.mockito.ArgumentMatchers.eq(GoalStatus.ACTIVE),
                org.mockito.ArgumentMatchers.eq(GoalCadence.WEEKLY), any(), any())).thenReturn(1L);

        service.create(new GoalRequest(hobbyId, "Segunda meta", "minutes", 120, "weekly", null, null));
        service.create(new GoalRequest(null, "Meta global", "sessions", 10, "custom",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31")));

        verify(entitlementService, org.mockito.Mockito.times(2)).requirePlus("user-1");
    }
}
