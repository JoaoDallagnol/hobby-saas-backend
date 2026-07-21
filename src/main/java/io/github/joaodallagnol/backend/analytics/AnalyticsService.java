package io.github.joaodallagnol.backend.analytics;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.gamification.SessionMetricRow;
import io.github.joaodallagnol.backend.gamification.UserBadgeRepository;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final long MAX_PERIOD_DAYS = 366;

    private final SessionRecordRepository sessionRepository;
    private final EntitlementService entitlementService;
    private final FeatureFlagService featureFlagService;
    private final UserBadgeRepository badgeRepository;
    private final Clock clock;

    public AnalyticsService(SessionRecordRepository sessionRepository, EntitlementService entitlementService,
                            FeatureFlagService featureFlagService, UserBadgeRepository badgeRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.entitlementService = entitlementService;
        this.featureFlagService = featureFlagService;
        this.badgeRepository = badgeRepository;
        this.clock = clock;
    }

    public InsightsResponse insights(LocalDate from, LocalDate to) {
        String userId = requirePlus();
        validatePeriod(from, to);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        List<SessionMetricRow> metrics = loadMetrics(userId, previousFrom, to);
        PeriodSummaryResponse current = summarize(metrics, from, to);
        PeriodSummaryResponse previous = summarize(metrics, previousFrom, previousTo);
        return new InsightsResponse(current, previous, current.sessions() - previous.sessions(),
                current.minutes() - previous.minutes(), current.activeDays() - previous.activeDays());
    }

    public WrappedResponse wrapped(Integer year, Integer month) {
        String userId = requirePlus();
        int currentYear = LocalDate.now(clock).getYear();
        int resolvedYear = year == null ? currentYear : year;
        if (resolvedYear < 1900 || resolvedYear > currentYear + 1) {
            throw new IllegalArgumentException("year must be between 1900 and next year.");
        }
        LocalDate from;
        LocalDate to;
        String period;
        if (month == null) {
            from = LocalDate.of(resolvedYear, 1, 1);
            to = LocalDate.of(resolvedYear, 12, 31);
            period = "year";
        } else {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("month must be between 1 and 12.");
            }
            YearMonth yearMonth = YearMonth.of(resolvedYear, month);
            from = yearMonth.atDay(1);
            to = yearMonth.atEndOfMonth();
            period = "month";
        }
        if (from.isAfter(LocalDate.now(clock).plusYears(1))) {
            throw new IllegalArgumentException("Wrapped period is too far in the future.");
        }
        LocalDate previousFrom = month == null ? from.minusYears(1) : from.minusMonths(1);
        LocalDate previousTo = month == null ? to.minusYears(1) : previousFrom.withDayOfMonth(previousFrom.lengthOfMonth());
        List<SessionMetricRow> metrics = loadMetrics(userId, previousFrom, to);
        List<WrappedBadgeResponse> badges = badgeRepository.findAllByUserIdOrderByEarnedAtDesc(userId).stream()
                .filter(badge -> {
                    LocalDate earned = badge.getEarnedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
                    return !earned.isBefore(from) && !earned.isAfter(to);
                })
                .map(badge -> new WrappedBadgeResponse(badge.getBadgeKey(),
                        badge.getHobby() == null ? null : badge.getHobby().getId(), badge.getEarnedAt()))
                .toList();
        return new WrappedResponse(period, summarize(metrics, from, to), summarize(metrics, previousFrom, previousTo),
                badges);
    }

    private String requirePlus() {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        entitlementService.requirePlus(userId);
        return userId;
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || days > MAX_PERIOD_DAYS) {
            throw new IllegalArgumentException("Period must contain between 1 and 366 days.");
        }
    }

    private List<SessionMetricRow> loadMetrics(String userId, LocalDate from, LocalDate to) {
        OffsetDateTime start = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endExclusive = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return sessionRepository.findGamificationMetricsByUserIdAndPeriod(userId, start, endExclusive);
    }

    private PeriodSummaryResponse summarize(List<SessionMetricRow> all, LocalDate from, LocalDate to) {
        List<SessionMetricRow> filtered = all.stream().filter(row -> {
            LocalDate date = row.startedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
            return !date.isBefore(from) && !date.isAfter(to);
        }).toList();
        Map<java.util.UUID, List<SessionMetricRow>> byHobby = filtered.stream()
                .collect(Collectors.groupingBy(SessionMetricRow::hobbyId));
        List<HobbyPeriodSummaryResponse> hobbies = byHobby.entrySet().stream().map(entry ->
                        new HobbyPeriodSummaryResponse(entry.getKey(), entry.getValue().getFirst().hobbyName(),
                                entry.getValue().size(), entry.getValue().stream()
                                .mapToLong(SessionMetricRow::durationMinutes).sum()))
                .sorted(Comparator.comparingLong(HobbyPeriodSummaryResponse::minutes).reversed()).toList();
        Map<LocalDate, List<SessionMetricRow>> byDate = filtered.stream().collect(Collectors.groupingBy(
                row -> row.startedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate(), LinkedHashMap::new,
                Collectors.toList()));
        List<DailyActivityResponse> daily = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyActivityResponse(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().mapToLong(SessionMetricRow::durationMinutes).sum()))
                .toList();
        return new PeriodSummaryResponse(from, to, filtered.size(),
                filtered.stream().mapToLong(SessionMetricRow::durationMinutes).sum(),
                filtered.stream().map(row -> row.startedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate())
                        .distinct().count(),
                filtered.stream().mapToInt(SessionMetricRow::durationMinutes).max().orElse(0), bestStreak(filtered),
                hobbies, daily);
    }

    private int bestStreak(List<SessionMetricRow> metrics) {
        List<LocalDate> dates = metrics.stream()
                .map(row -> row.startedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate())
                .distinct().sorted().toList();
        int current = 0;
        int best = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            best = Math.max(best, current);
            previous = date;
        }
        return best;
    }
}
