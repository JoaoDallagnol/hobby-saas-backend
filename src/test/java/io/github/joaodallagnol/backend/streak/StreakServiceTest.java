package io.github.joaodallagnol.backend.streak;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class StreakServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCountConsecutiveUniqueUtcDays() {
        authenticate("firebase-user-1", "user@example.com", "User");
        List<SessionRecord> storage = new ArrayList<>();
        storage.add(createSession("firebase-user-1", OffsetDateTime.parse("2026-07-18T07:30:00Z")));
        storage.add(createSession("firebase-user-1", OffsetDateTime.parse("2026-07-18T18:30:00Z")));
        storage.add(createSession("firebase-user-1", OffsetDateTime.parse("2026-07-17T07:30:00Z")));
        storage.add(createSession("firebase-user-1", OffsetDateTime.parse("2026-07-16T07:30:00Z")));

        StreakService service = new StreakService(
                new AuthenticatedUserExtractor(),
                repositoryFor(storage),
                Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC)
        );

        StreakResponse response = service.getCurrentUserStreak();

        assertThat(response.currentStreakDays()).isEqualTo(3);
        assertThat(response.lastActiveDateUtc()).isEqualTo(java.time.LocalDate.parse("2026-07-18"));
        assertThat(response.activeToday()).isTrue();
    }

    @Test
    void shouldKeepStreakActiveWhenLastSessionWasYesterday() {
        authenticate("firebase-user-1", "user@example.com", "User");
        List<SessionRecord> storage = List.of(
                createSession("firebase-user-1", OffsetDateTime.parse("2026-07-17T07:30:00Z")),
                createSession("firebase-user-1", OffsetDateTime.parse("2026-07-16T07:30:00Z"))
        );

        StreakService service = new StreakService(
                new AuthenticatedUserExtractor(),
                repositoryFor(storage),
                Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC)
        );

        StreakResponse response = service.getCurrentUserStreak();

        assertThat(response.currentStreakDays()).isEqualTo(2);
        assertThat(response.activeToday()).isFalse();
    }

    @Test
    void shouldResetCurrentStreakWhenGapIsOlderThanYesterday() {
        authenticate("firebase-user-1", "user@example.com", "User");
        List<SessionRecord> storage = List.of(
                createSession("firebase-user-1", OffsetDateTime.parse("2026-07-15T07:30:00Z")),
                createSession("firebase-user-1", OffsetDateTime.parse("2026-07-14T07:30:00Z"))
        );

        StreakService service = new StreakService(
                new AuthenticatedUserExtractor(),
                repositoryFor(storage),
                Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC)
        );

        StreakResponse response = service.getCurrentUserStreak();

        assertThat(response.currentStreakDays()).isZero();
        assertThat(response.lastActiveDateUtc()).isEqualTo(java.time.LocalDate.parse("2026-07-15"));
    }

    @Test
    void shouldReturnZeroWhenUserHasNoSessions() {
        authenticate("firebase-user-1", "user@example.com", "User");
        StreakService service = new StreakService(
                new AuthenticatedUserExtractor(),
                repositoryFor(List.of()),
                Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC)
        );

        StreakResponse response = service.getCurrentUserStreak();

        assertThat(response.currentStreakDays()).isZero();
        assertThat(response.lastActiveDateUtc()).isNull();
    }

    private SessionRecordRepository repositoryFor(List<SessionRecord> storage) {
        return (SessionRecordRepository) Proxy.newProxyInstance(
                SessionRecordRepository.class.getClassLoader(),
                new Class<?>[]{SessionRecordRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findStartedAtByUserIdOrderByStartedAtDesc" -> storage.stream()
                            .filter(item -> item.getUserId().equals(args[0]))
                            .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                            .map(SessionRecord::getStartedAt)
                            .toList();
                    case "findByIdAndUserId" -> java.util.Optional.empty();
                    case "save" -> args[0];
                    case "delete" -> null;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestSessionRecordRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private SessionRecord createSession(String userId, OffsetDateTime startedAt) {
        Hobby hobby = ReflectionFactory.createHobby(UUID.randomUUID(), "Running", "Sports & Movement");
        return new SessionRecord(
                userId,
                hobby,
                "Session",
                startedAt,
                30,
                null,
                4,
                null,
                null,
                null
        );
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class ReflectionFactory {
        static Hobby createHobby(UUID id, String name, String categoryName) {
            try {
                var categoryConstructor = HobbyCategory.class.getDeclaredConstructor();
                categoryConstructor.setAccessible(true);
                HobbyCategory category = categoryConstructor.newInstance();
                setField(HobbyCategory.class, category, "id", UUID.randomUUID());
                setField(HobbyCategory.class, category, "name", categoryName);

                var hobbyConstructor = Hobby.class.getDeclaredConstructor();
                hobbyConstructor.setAccessible(true);
                Hobby hobby = hobbyConstructor.newInstance();
                setField(Hobby.class, hobby, "id", id);
                setField(Hobby.class, hobby, "name", name);
                setField(Hobby.class, hobby, "icon", "icon");
                setField(Hobby.class, hobby, "category", category);
                return hobby;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static void setField(Class<?> type, Object target, String fieldName, Object value) throws Exception {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
