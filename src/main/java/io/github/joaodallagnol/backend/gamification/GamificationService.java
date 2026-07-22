package io.github.joaodallagnol.backend.gamification;

import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.config.UserDerivedDataCache;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.support.NoOpCacheManager;

@Service
public class GamificationService {

    private static final int MONTHLY_CHALLENGE_TARGET = 8;
    private static final Map<String, BadgeDefinition> BADGES = badgeCatalog();

    private final SessionRecordRepository sessionRepository;
    private final HobbyXpRepository hobbyXpRepository;
    private final UserBadgeRepository badgeRepository;
    private final HobbyRepository hobbyRepository;
    private final EntitlementService entitlementService;
    private final FeatureFlagService featureFlagService;
    private final Clock clock;
    private final UserDerivedDataCache userDerivedDataCache;

    @Autowired
    public GamificationService(SessionRecordRepository sessionRepository, HobbyXpRepository hobbyXpRepository,
                               UserBadgeRepository badgeRepository, HobbyRepository hobbyRepository,
                               EntitlementService entitlementService, FeatureFlagService featureFlagService,
                               Clock clock, UserDerivedDataCache userDerivedDataCache) {
        this.sessionRepository = sessionRepository;
        this.hobbyXpRepository = hobbyXpRepository;
        this.badgeRepository = badgeRepository;
        this.hobbyRepository = hobbyRepository;
        this.entitlementService = entitlementService;
        this.featureFlagService = featureFlagService;
        this.clock = clock;
        this.userDerivedDataCache = userDerivedDataCache;
    }

    public GamificationService(SessionRecordRepository sessionRepository, HobbyXpRepository hobbyXpRepository,
                               UserBadgeRepository badgeRepository, HobbyRepository hobbyRepository,
                               EntitlementService entitlementService, FeatureFlagService featureFlagService,
                               Clock clock) {
        this(sessionRepository, hobbyXpRepository, badgeRepository, hobbyRepository, entitlementService,
                featureFlagService, clock, new UserDerivedDataCache(new NoOpCacheManager()));
    }

    @Transactional
    public GamificationDashboardResponse dashboard() {
        featureFlagService.requireGamification();
        String userId = entitlementService.currentUserId();
        GamificationDashboardResponse cached = userDerivedDataCache.getDashboard(userId);
        if (cached != null) {
            return cached;
        }
        hobbyXpRepository.lockUserProjection("gamification:" + userId);
        List<SessionMetricRow> metrics = sessionRepository.findGamificationMetricsByUserId(userId);
        PersonalRecordsResponse records = calculateRecords(metrics);
        MonthlyChallengeResponse challenge = calculateMonthlyChallenge(metrics);
        List<HobbyProgressResponse> progress = rebuildXp(userId, metrics);
        List<BadgeResponse> badges = awardAndReadBadges(userId, metrics, records, challenge);
        GamificationDashboardResponse response = new GamificationDashboardResponse(progress, badges, records, challenge);
        userDerivedDataCache.putDashboardAfterRebuild(userId, response);
        return response;
    }

    private List<HobbyProgressResponse> rebuildXp(String userId, List<SessionMetricRow> metrics) {
        Map<UUID, List<SessionMetricRow>> byHobby = metrics.stream()
                .collect(Collectors.groupingBy(SessionMetricRow::hobbyId, LinkedHashMap::new, Collectors.toList()));
        hobbyXpRepository.deleteAllByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<HobbyProgressResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, List<SessionMetricRow>> entry : byHobby.entrySet()) {
            List<SessionMetricRow> sessions = entry.getValue();
            int xp = sessions.stream().mapToInt(row -> row.xpSessionBonus()
                    + row.durationMinutes() / row.xpMinutesPerPoint()).sum();
            LevelInfo level = levelFor(xp);
            hobbyXpRepository.save(new HobbyXp(userId, entry.getKey(), xp, level.level(), level.label(), now));
            result.add(new HobbyProgressResponse(entry.getKey(), sessions.getFirst().hobbyName(), xp, level.level(),
                    level.label(), level.nextThreshold()));
        }
        return result.stream().sorted(Comparator.comparingInt(HobbyProgressResponse::xp).reversed()).toList();
    }

    private List<BadgeResponse> awardAndReadBadges(String userId, List<SessionMetricRow> metrics,
                                                    PersonalRecordsResponse records,
                                                    MonthlyChallengeResponse challenge) {
        List<UserBadge> existing = new ArrayList<>(badgeRepository.findAllByUserIdOrderByEarnedAtDesc(userId));
        Set<String> existingScopes = existing.stream().map(this::scopeKey).collect(Collectors.toSet());
        OffsetDateTime now = OffsetDateTime.now(clock);
        long totalSessions = metrics.size();
        long totalMinutes = metrics.stream().mapToLong(SessionMetricRow::durationMinutes).sum();
        awardGlobal(existing, existingScopes, userId, "first_session", totalSessions >= 1, now);
        awardGlobal(existing, existingScopes, userId, "sessions_5", totalSessions >= 5, now);
        awardGlobal(existing, existingScopes, userId, "sessions_25", totalSessions >= 25, now);
        awardGlobal(existing, existingScopes, userId, "sessions_100", totalSessions >= 100, now);
        awardGlobal(existing, existingScopes, userId, "hours_10", totalMinutes >= 600, now);
        awardGlobal(existing, existingScopes, userId, "hours_50", totalMinutes >= 3000, now);
        awardGlobal(existing, existingScopes, userId, "explorer_3",
                metrics.stream().map(SessionMetricRow::hobbyId).distinct().count() >= 3, now);
        awardGlobal(existing, existingScopes, userId, "streak_7", records.bestStreakDays() >= 7, now);
        awardGlobal(existing, existingScopes, userId, "streak_30", records.bestStreakDays() >= 30, now);
        awardGlobal(existing, existingScopes, userId, "monthly_challenge_first", challenge.achieved(), now);

        metrics.stream().collect(Collectors.groupingBy(SessionMetricRow::hobbyId, Collectors.counting()))
                .forEach((hobbyId, count) -> {
                    if (count >= 10 && existingScopes.add("hobby_sessions_10:" + hobbyId)) {
                        Hobby hobby = hobbyRepository.getReferenceById(hobbyId);
                        existing.add(badgeRepository.save(new UserBadge(userId, "hobby_sessions_10", hobby, now)));
                    }
                });

        return existing.stream().sorted(Comparator.comparing(UserBadge::getEarnedAt).reversed())
                .map(this::toBadgeResponse).toList();
    }

    private void awardGlobal(List<UserBadge> badges, Set<String> scopes, String userId, String key,
                             boolean eligible, OffsetDateTime now) {
        if (eligible && scopes.add(key + ":global")) {
            badges.add(badgeRepository.save(new UserBadge(userId, key, null, now)));
        }
    }

    private BadgeResponse toBadgeResponse(UserBadge badge) {
        BadgeDefinition definition = BADGES.getOrDefault(badge.getBadgeKey(),
                new BadgeDefinition(badge.getBadgeKey(), badge.getBadgeKey()));
        return new BadgeResponse(badge.getId(), badge.getBadgeKey(), definition.name(), definition.description(),
                badge.getHobby() == null ? null : badge.getHobby().getId(),
                badge.getHobby() == null ? null : badge.getHobby().getName(), badge.getEarnedAt());
    }

    private String scopeKey(UserBadge badge) {
        return badge.getBadgeKey() + ":" + (badge.getHobby() == null ? "global" : badge.getHobby().getId());
    }

    private PersonalRecordsResponse calculateRecords(List<SessionMetricRow> metrics) {
        if (metrics.isEmpty()) {
            return new PersonalRecordsResponse(0, 0, 0, 0, null, null);
        }
        int longest = metrics.stream().mapToInt(SessionMetricRow::durationMinutes).max().orElse(0);
        WeekFields weekFields = WeekFields.ISO;
        Map<String, Long> weekly = metrics.stream().collect(Collectors.groupingBy(row -> {
            LocalDate date = utcDate(row.startedAt());
            return date.get(weekFields.weekBasedYear()) + "-" + date.get(weekFields.weekOfWeekBasedYear());
        }, Collectors.counting()));
        Map<YearMonth, Integer> monthlyMinutes = metrics.stream().collect(Collectors.groupingBy(
                row -> YearMonth.from(utcDate(row.startedAt())), Collectors.summingInt(SessionMetricRow::durationMinutes)));
        Map<UUID, List<SessionMetricRow>> byHobby = metrics.stream().collect(Collectors.groupingBy(SessionMetricRow::hobbyId));
        HobbyRecordResponse topSessions = byHobby.entrySet().stream()
                .max(Comparator.comparingLong(entry -> entry.getValue().size()))
                .map(entry -> new HobbyRecordResponse(entry.getKey(), entry.getValue().getFirst().hobbyName(),
                        entry.getValue().size())).orElse(null);
        HobbyRecordResponse topMinutes = byHobby.entrySet().stream()
                .max(Comparator.comparingLong(entry -> entry.getValue().stream()
                        .mapToLong(SessionMetricRow::durationMinutes).sum()))
                .map(entry -> new HobbyRecordResponse(entry.getKey(), entry.getValue().getFirst().hobbyName(),
                        entry.getValue().stream().mapToLong(SessionMetricRow::durationMinutes).sum())).orElse(null);
        return new PersonalRecordsResponse(longest, weekly.values().stream().mapToLong(Long::longValue).max().orElse(0),
                monthlyMinutes.values().stream().mapToLong(Integer::longValue).max().orElse(0), bestStreak(metrics),
                topSessions, topMinutes);
    }

    private int bestStreak(List<SessionMetricRow> metrics) {
        List<LocalDate> dates = metrics.stream().map(row -> utcDate(row.startedAt())).distinct().sorted().toList();
        int best = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            best = Math.max(best, current);
            previous = date;
        }
        return best;
    }

    private MonthlyChallengeResponse calculateMonthlyChallenge(List<SessionMetricRow> metrics) {
        YearMonth month = YearMonth.now(clock);
        long progress = metrics.stream().filter(row -> YearMonth.from(utcDate(row.startedAt())).equals(month)).count();
        return new MonthlyChallengeResponse("monthly_sessions_8", "Practice on eight occasions", month, "sessions",
                MONTHLY_CHALLENGE_TARGET, progress,
                (int) Math.min(100, progress * 100 / MONTHLY_CHALLENGE_TARGET), progress >= MONTHLY_CHALLENGE_TARGET);
    }

    private LocalDate utcDate(OffsetDateTime value) {
        return value.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private LevelInfo levelFor(int xp) {
        if (xp >= 1500) return new LevelInfo(5, "Experienced", null);
        if (xp >= 750) return new LevelInfo(4, "Dedicated", 1500);
        if (xp >= 300) return new LevelInfo(3, "Consistent", 750);
        if (xp >= 100) return new LevelInfo(2, "Engaged", 300);
        return new LevelInfo(1, "Beginner", 100);
    }

    private static Map<String, BadgeDefinition> badgeCatalog() {
        Map<String, BadgeDefinition> badges = new HashMap<>();
        badges.put("first_session", new BadgeDefinition("First Step", "Recorded the first hobby session."));
        badges.put("sessions_5", new BadgeDefinition("Getting Started", "Recorded five sessions."));
        badges.put("sessions_25", new BadgeDefinition("Committed", "Recorded twenty-five sessions."));
        badges.put("sessions_100", new BadgeDefinition("Centurion", "Recorded one hundred sessions."));
        badges.put("hours_10", new BadgeDefinition("Ten Hours", "Practiced for ten accumulated hours."));
        badges.put("hours_50", new BadgeDefinition("Fifty Hours", "Practiced for fifty accumulated hours."));
        badges.put("explorer_3", new BadgeDefinition("Explorer", "Recorded sessions in three different hobbies."));
        badges.put("streak_7", new BadgeDefinition("Consistent Week", "Reached a seven-day streak."));
        badges.put("streak_30", new BadgeDefinition("Consistent Month", "Reached a thirty-day streak."));
        badges.put("hobby_sessions_10", new BadgeDefinition("Hobby Regular", "Recorded ten sessions in one hobby."));
        badges.put("monthly_challenge_first", new BadgeDefinition("Challenge Accepted", "Completed a monthly challenge."));
        return Map.copyOf(badges);
    }

    private record LevelInfo(int level, String label, Integer nextThreshold) {
    }

    private record BadgeDefinition(String name, String description) {
    }
}
