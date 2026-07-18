package io.github.joaodallagnol.backend.streak;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class StreakService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final SessionRecordRepository sessionRecordRepository;
    private final Clock clock;

    public StreakService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            SessionRecordRepository sessionRecordRepository,
            Clock clock
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.sessionRecordRepository = sessionRecordRepository;
        this.clock = clock;
    }

    public StreakResponse getCurrentUserStreak() {
        String userId = authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication()).id();
        List<SessionRecord> sessions = sessionRecordRepository.findAllByUserIdOrderByStartedAtDesc(userId);
        LocalDate referenceDate = LocalDate.now(clock);

        if (sessions.isEmpty()) {
            return new StreakResponse(0, null, referenceDate, false);
        }

        Set<LocalDate> uniqueDates = new LinkedHashSet<>();
        for (SessionRecord session : sessions) {
            uniqueDates.add(session.getStartedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate());
        }

        List<LocalDate> orderedDates = uniqueDates.stream().sorted((a, b) -> b.compareTo(a)).toList();
        LocalDate lastActiveDate = orderedDates.getFirst();
        LocalDate yesterday = referenceDate.minusDays(1);

        if (lastActiveDate.isBefore(yesterday)) {
            return new StreakResponse(0, lastActiveDate, referenceDate, false);
        }

        int streak = 1;
        LocalDate expected = lastActiveDate.minusDays(1);
        for (int i = 1; i < orderedDates.size(); i++) {
            if (orderedDates.get(i).equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }

        return new StreakResponse(
                streak,
                lastActiveDate,
                referenceDate,
                lastActiveDate.equals(referenceDate)
        );
    }
}
