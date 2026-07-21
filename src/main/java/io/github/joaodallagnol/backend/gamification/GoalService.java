package io.github.joaodallagnol.backend.gamification;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GoalService {

    private static final long MAX_CUSTOM_DAYS = 366;

    private final GoalRepository goalRepository;
    private final SessionRecordRepository sessionRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final EntitlementService entitlementService;
    private final FeatureFlagService featureFlagService;
    private final Clock clock;

    public GoalService(GoalRepository goalRepository, SessionRecordRepository sessionRepository,
                       HobbyRepository hobbyRepository, UserHobbyRepository userHobbyRepository,
                       EntitlementService entitlementService, FeatureFlagService featureFlagService, Clock clock) {
        this.goalRepository = goalRepository;
        this.sessionRepository = sessionRepository;
        this.hobbyRepository = hobbyRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.entitlementService = entitlementService;
        this.featureFlagService = featureFlagService;
        this.clock = clock;
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        GoalMetric metric = GoalMetric.from(request.metric());
        GoalCadence cadence = GoalCadence.from(request.cadence());
        Hobby hobby = resolveHobby(userId, request.hobbyId());
        Period period = resolvePeriod(cadence, request.startDate(), request.endDate());
        boolean advanced = requiresPlus(userId, hobby, cadence, period, null);
        if (advanced) {
            entitlementService.requirePlus(userId);
        }
        Goal goal = new Goal(userId, hobby, request.name().trim(), metric, request.targetValue(), cadence,
                period.start(), period.end(), advanced, OffsetDateTime.now(clock));
        return toResponse(goalRepository.save(goal));
    }

    public List<GoalResponse> list() {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        return goalRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse update(UUID goalId, GoalRequest request) {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        Goal goal = ownedGoal(goalId, userId);
        if (goal.isAdvanced()) {
            entitlementService.requirePlus(userId);
        }
        GoalCadence cadence = GoalCadence.from(request.cadence());
        Hobby hobby = resolveHobby(userId, request.hobbyId());
        Period period = resolvePeriod(cadence, request.startDate(), request.endDate());
        boolean advanced = requiresPlus(userId, hobby, cadence, period, goalId);
        if (advanced) {
            entitlementService.requirePlus(userId);
        }
        goal.update(hobby, request.name().trim(), GoalMetric.from(request.metric()), request.targetValue(), cadence,
                period.start(), period.end(), advanced);
        return toResponse(goal);
    }

    @Transactional
    public void archive(UUID goalId) {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        ownedGoal(goalId, userId).archive();
    }

    private Goal ownedGoal(UUID id, String userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found."));
    }

    private Hobby resolveHobby(String userId, UUID hobbyId) {
        if (hobbyId == null) {
            return null;
        }
        if (!userHobbyRepository.existsByIdUserIdAndIdHobbyId(userId, hobbyId)) {
            throw new IllegalArgumentException("Hobby is not linked to the user profile.");
        }
        return hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby not found."));
    }

    private boolean requiresPlus(String userId, Hobby hobby, GoalCadence cadence, Period period,
                                 UUID goalBeingUpdated) {
        if (hobby == null || cadence != GoalCadence.WEEKLY) {
            return true;
        }
        goalRepository.lockScope(userId + ":" + hobby.getId());
        long active = goalRepository.countOverlapping(userId, hobby.getId(), GoalStatus.ACTIVE, GoalCadence.WEEKLY,
                period.start(), period.end());
        if (goalBeingUpdated != null) {
            Goal current = goalRepository.findByIdAndUserId(goalBeingUpdated, userId).orElse(null);
            if (current != null && current.getStatus() == GoalStatus.ACTIVE && current.getHobby() != null
                    && current.getHobby().getId().equals(hobby.getId())
                    && current.getCadence() == GoalCadence.WEEKLY
                    && !current.getStartDate().isAfter(period.end())
                    && !current.getEndDate().isBefore(period.start())) {
                active--;
            }
        }
        return active > 0;
    }

    private Period resolvePeriod(GoalCadence cadence, LocalDate requestedStart, LocalDate requestedEnd) {
        LocalDate today = LocalDate.now(clock);
        if (cadence == GoalCadence.WEEKLY) {
            LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new Period(start, start.plusDays(6));
        }
        if (cadence == GoalCadence.MONTHLY) {
            return new Period(today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth()));
        }
        if (requestedStart == null || requestedEnd == null) {
            throw new IllegalArgumentException("startDate and endDate are required for custom cadence.");
        }
        if (requestedEnd.isBefore(requestedStart) || requestedStart.plusDays(MAX_CUSTOM_DAYS - 1).isBefore(requestedEnd)) {
            throw new IllegalArgumentException("Custom goal period must contain between 1 and 366 days.");
        }
        return new Period(requestedStart, requestedEnd);
    }

    private GoalResponse toResponse(Goal goal) {
        OffsetDateTime start = goal.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endExclusive = goal.getEndDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        UUID hobbyId = goal.getHobby() == null ? null : goal.getHobby().getId();
        long progress = goal.getMetric() == GoalMetric.SESSIONS
                ? countSessions(goal.getUserId(), hobbyId, start, endExclusive)
                : sessionRepository.sumDurationMinutes(goal.getUserId(), hobbyId, start, endExclusive);
        int percent = (int) Math.min(100, progress * 100L / goal.getTargetValue());
        return new GoalResponse(goal.getId(), hobbyId, goal.getHobby() == null ? null : goal.getHobby().getName(),
                goal.getName(), goal.getMetric().value(), goal.getTargetValue(), goal.getCadence().value(),
                goal.getStartDate(), goal.getEndDate(), goal.getStatus().value(), goal.isAdvanced(), progress, percent,
                progress >= goal.getTargetValue());
    }

    private long countSessions(String userId, UUID hobbyId, OffsetDateTime start, OffsetDateTime end) {
        return hobbyId == null
                ? sessionRepository.countByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(userId, start, end)
                : sessionRepository.countByUserIdAndHobbyIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        userId, hobbyId, start, end);
    }

    private record Period(LocalDate start, LocalDate end) {
    }
}
