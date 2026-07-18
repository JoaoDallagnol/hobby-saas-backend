package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SessionServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateListGetUpdateAndDeleteSession() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryEquipmentReferenceRepository equipmentRepository = new InMemoryEquipmentReferenceRepository();
        InMemoryBacklogItemReferenceRepository backlogRepository = new InMemoryBacklogItemReferenceRepository();
        InMemoryPlaceReferenceRepository placeRepository = new InMemoryPlaceReferenceRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        EquipmentReference equipment = equipmentRepository.create("firebase-user-1");
        UUID projectId = backlogRepository.create("firebase-user-1");
        placeRepository.add("place-123");

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                equipmentRepository.asRepository(),
                backlogRepository.asRepository(),
                placeRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        SessionResponse created = service.createSession(new CreateSessionRequest(
                hobby.getId(),
                "Morning Run",
                OffsetDateTime.parse("2026-07-18T07:30:00Z"),
                45,
                "Good pace",
                4,
                new SessionLocationRequest("place-123"),
                projectId,
                List.of(equipment.getId()),
                List.of(new SessionPhotoRequest("uploads/user/session1/photo1.webp")),
                Map.of("distance_km", 8.5)
        ));

        List<SessionResponse> listed = service.listSessions(null);
        SessionResponse detailed = service.getSession(created.id());
        SessionResponse updated = service.updateSession(created.id(), new UpdateSessionRequest(
                hobby.getId(),
                "Evening Run",
                OffsetDateTime.parse("2026-07-18T18:30:00Z"),
                50,
                "Better finish",
                5,
                new SessionLocationRequest("place-123"),
                projectId,
                List.of(equipment.getId()),
                List.of(new SessionPhotoRequest("uploads/user/session1/photo2.webp")),
                Map.of("distance_km", 9.0)
        ));
        service.deleteSession(created.id());

        assertThat(created.title()).isEqualTo("Morning Run");
        assertThat(created.equipmentIds()).containsExactly(equipment.getId());
        assertThat(created.photos()).hasSize(1);
        assertThat(created.attributes()).containsEntry("distance_km", 8.5);
        assertThat(listed).hasSize(1);
        assertThat(detailed.id()).isEqualTo(created.id());
        assertThat(updated.title()).isEqualTo("Evening Run");
        assertThat(updated.satisfaction()).isEqualTo(5);
        assertThat(service.listSessions(null)).isEmpty();
    }

    @Test
    void shouldRejectSessionForHobbyOutsideUserProfile() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        hobbies.createHobby(hobbyId, "Running", "Sports & Movement");

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                new InMemoryUserHobbyRepository().asRepository(),
                new InMemoryEquipmentReferenceRepository().asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.createSession(new CreateSessionRequest(
                hobbyId,
                "Run",
                OffsetDateTime.parse("2026-07-18T07:30:00Z"),
                45,
                null,
                4,
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not linked");
    }

    @Test
    void shouldRejectEquipmentOwnedByAnotherUser() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryEquipmentReferenceRepository equipmentRepository = new InMemoryEquipmentReferenceRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        EquipmentReference equipment = equipmentRepository.create("other-user");

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                equipmentRepository.asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.createSession(new CreateSessionRequest(
                hobbyId,
                "Run",
                OffsetDateTime.parse("2026-07-18T07:30:00Z"),
                45,
                null,
                4,
                null,
                null,
                List.of(equipment.getId()),
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equipment ids are invalid");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class InMemorySessionRecordRepository {
        private final Map<UUID, SessionRecord> storage = new HashMap<>();

        SessionRecordRepository asRepository() {
            return (SessionRecordRepository) Proxy.newProxyInstance(
                    SessionRecordRepository.class.getClassLoader(),
                    new Class<?>[]{SessionRecordRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> {
                            SessionRecord session = (SessionRecord) args[0];
                            storage.put(session.getId(), session);
                            yield session;
                        }
                        case "delete" -> {
                            storage.remove(((SessionRecord) args[0]).getId());
                            yield null;
                        }
                        case "findByIdAndUserId" -> storage.values().stream()
                                .filter(item -> item.getId().equals(args[0]) && item.getUserId().equals(args[1]))
                                .findFirst();
                        case "findAllByUserIdOrderByStartedAtDesc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]))
                                .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                .toList();
                        case "findAllByUserIdAndHobbyIdOrderByStartedAtDesc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]) && item.getHobby().getId().equals(args[1]))
                                .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                .toList();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemorySessionRecordRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryHobbyRepository {
        private final Map<UUID, Hobby> storage = new HashMap<>();

        Hobby createHobby(UUID id, String name, String categoryName) {
            HobbyCategory category = ReflectionFactory.createHobbyCategory(UUID.randomUUID(), categoryName);
            Hobby hobby = ReflectionFactory.createHobby(id, name, category);
            storage.put(id, hobby);
            return hobby;
        }

        HobbyRepository asRepository() {
            return (HobbyRepository) Proxy.newProxyInstance(
                    HobbyRepository.class.getClassLoader(),
                    new Class<?>[]{HobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(storage.get((UUID) args[0]));
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryHobbyRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryUserHobbyRepository {
        private final Set<String> linkedPairs = new LinkedHashSet<>();

        void link(String userId, UUID hobbyId) {
            linkedPairs.add(userId + "|" + hobbyId);
        }

        UserHobbyRepository asRepository() {
            return (UserHobbyRepository) Proxy.newProxyInstance(
                    UserHobbyRepository.class.getClassLoader(),
                    new Class<?>[]{UserHobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByIdUserIdAndIdHobbyId" -> linkedPairs.contains(args[0] + "|" + args[1]);
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryUserHobbyRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryEquipmentReferenceRepository {
        private final Map<UUID, EquipmentReference> storage = new HashMap<>();

        EquipmentReference create(String userId) {
            EquipmentReference equipment = ReflectionFactory.createEquipment(UUID.randomUUID(), userId);
            storage.put(equipment.getId(), equipment);
            return equipment;
        }

        EquipmentReferenceRepository asRepository() {
            return (EquipmentReferenceRepository) Proxy.newProxyInstance(
                    EquipmentReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{EquipmentReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByIdInAndUserId" -> {
                            @SuppressWarnings("unchecked")
                            Set<UUID> ids = (Set<UUID>) args[0];
                            String userId = (String) args[1];
                            yield storage.values().stream()
                                    .filter(item -> ids.contains(item.getId()) && item.getUserId().equals(userId))
                                    .toList();
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryEquipmentReferenceRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryBacklogItemReferenceRepository {
        private final Map<UUID, BacklogItemReference> storage = new HashMap<>();

        UUID create(String userId) {
            BacklogItemReference backlogItem = ReflectionFactory.createBacklogItem(UUID.randomUUID(), userId);
            storage.put(backlogItem.getId(), backlogItem);
            return backlogItem.getId();
        }

        BacklogItemReferenceRepository asRepository() {
            return (BacklogItemReferenceRepository) Proxy.newProxyInstance(
                    BacklogItemReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{BacklogItemReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByIdAndUserId" -> {
                            BacklogItemReference item = storage.get((UUID) args[0]);
                            yield item != null && item.getUserId().equals(args[1]);
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryBacklogItemReferenceRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryPlaceReferenceRepository {
        private final Set<String> placeIds = new LinkedHashSet<>();

        void add(String placeId) {
            placeIds.add(placeId);
        }

        PlaceReferenceRepository asRepository() {
            return (PlaceReferenceRepository) Proxy.newProxyInstance(
                    PlaceReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{PlaceReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsById" -> placeIds.contains(args[0]);
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryPlaceReferenceRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class ReflectionFactory {
        static HobbyCategory createHobbyCategory(UUID id, String name) {
            try {
                var constructor = HobbyCategory.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                HobbyCategory category = constructor.newInstance();
                setField(HobbyCategory.class, category, "id", id);
                setField(HobbyCategory.class, category, "name", name);
                return category;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static Hobby createHobby(UUID id, String name, HobbyCategory category) {
            try {
                var constructor = Hobby.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                Hobby hobby = constructor.newInstance();
                setField(Hobby.class, hobby, "id", id);
                setField(Hobby.class, hobby, "name", name);
                setField(Hobby.class, hobby, "icon", "icon");
                setField(Hobby.class, hobby, "category", category);
                return hobby;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static EquipmentReference createEquipment(UUID id, String userId) {
            try {
                var constructor = EquipmentReference.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                EquipmentReference equipment = constructor.newInstance();
                setField(EquipmentReference.class, equipment, "id", id);
                setField(EquipmentReference.class, equipment, "userId", userId);
                return equipment;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static BacklogItemReference createBacklogItem(UUID id, String userId) {
            try {
                var constructor = BacklogItemReference.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                BacklogItemReference item = constructor.newInstance();
                setField(BacklogItemReference.class, item, "id", id);
                setField(BacklogItemReference.class, item, "userId", userId);
                return item;
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
