package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.storage.SessionPhotoStorageKeyPolicy;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
        InMemoryHobbyAttributeTemplateRepository templateRepository = new InMemoryHobbyAttributeTemplateRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        EquipmentReference equipment = equipmentRepository.create("firebase-user-1");
        UUID projectId = backlogRepository.create("firebase-user-1");
        placeRepository.add("place-123");
        templateRepository.add(hobby, UUID.fromString("2f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "distance_km", "Distance", "number", "km", 1);

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                equipmentRepository.asRepository(),
                backlogRepository.asRepository(),
                placeRepository.asRepository(),
                new HobbyAttributeTemplateService(
                        new AuthenticatedUserExtractor(),
                        hobbies.asRepository(),
                        userHobbies.asRepository(),
                        templateRepository.asRepository()
                ),
                new PlaceResolutionService(placeRepository.asRepository(), placeId -> {
                    throw new UnsupportedOperationException("Should not fetch uncached place in this test.");
                }),
                enabledFeatureFlags()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        SessionResponse created = service.createSession(new CreateSessionRequest(
                hobby.getId(),
                "Morning Run",
                OffsetDateTime.parse("2026-07-18T07:30:00Z"),
                45,
                "Good pace",
                4,
                new SessionLocationRequest("place-123", "Central Park"),
                projectId,
                List.of(equipment.getId()),
                List.of(new SessionPhotoRequest(SessionPhotoStorageKeyPolicy.uploadPrefix("firebase-user-1") + "photo1.webp")),
                Map.of("distance_km", 8.5)
        ));

        SessionPageResponse listed = service.listSessions(null, 0, 20);
        SessionResponse detailed = service.getSession(created.id());
        SessionResponse updated = service.updateSession(created.id(), new UpdateSessionRequest(
                hobby.getId(),
                "Evening Run",
                OffsetDateTime.parse("2026-07-18T18:30:00Z"),
                50,
                "Better finish",
                5,
                new SessionLocationRequest("place-123", "Central Park"),
                projectId,
                List.of(equipment.getId()),
                List.of(new SessionPhotoRequest(SessionPhotoStorageKeyPolicy.uploadPrefix("firebase-user-1") + "photo2.webp")),
                Map.of("distance_km", 9.0)
        ));
        service.deleteSession(created.id());

        assertThat(created.title()).isEqualTo("Morning Run");
        assertThat(created.equipmentIds()).containsExactly(equipment.getId());
        assertThat(created.photos()).hasSize(1);
        assertThat(created.attributes()).containsEntry("distance_km", 8.5);
        assertThat(listed.items()).hasSize(1);
        assertThat(detailed.id()).isEqualTo(created.id());
        assertThat(updated.title()).isEqualTo("Evening Run");
        assertThat(updated.satisfaction()).isEqualTo(5);
        assertThat(service.listSessions(null, 0, 20).items()).isEmpty();
    }

    @Test
    void shouldRejectSessionForHobbyOutsideUserProfile() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryHobbyAttributeTemplateRepository templateRepository = new InMemoryHobbyAttributeTemplateRepository();
        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        hobbies.createHobby(hobbyId, "Running", "Sports & Movement");

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                new InMemoryEquipmentReferenceRepository().asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository(),
                new HobbyAttributeTemplateService(
                        new AuthenticatedUserExtractor(),
                        hobbies.asRepository(),
                        userHobbies.asRepository(),
                        templateRepository.asRepository()
                ),
                new PlaceResolutionService(new InMemoryPlaceReferenceRepository().asRepository(), placeId -> {
                    throw new UnsupportedOperationException("No place resolution expected.");
                }),
                enabledFeatureFlags()
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
        InMemoryHobbyAttributeTemplateRepository templateRepository = new InMemoryHobbyAttributeTemplateRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        EquipmentReference equipment = equipmentRepository.create("other-user");
        templateRepository.add(hobby, UUID.fromString("2f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "distance_km", "Distance", "number", "km", 1);

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                equipmentRepository.asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository(),
                new HobbyAttributeTemplateService(
                        new AuthenticatedUserExtractor(),
                        hobbies.asRepository(),
                        userHobbies.asRepository(),
                        templateRepository.asRepository()
                ),
                new PlaceResolutionService(new InMemoryPlaceReferenceRepository().asRepository(), placeId -> {
                    throw new UnsupportedOperationException("No place resolution expected.");
                }),
                enabledFeatureFlags()
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

    @Test
    void shouldRejectAttributeKeyOutsideHobbyTemplate() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryHobbyAttributeTemplateRepository templateRepository = new InMemoryHobbyAttributeTemplateRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        templateRepository.add(hobby, UUID.fromString("2f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "distance_km", "Distance", "number", "km", 1);

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                new InMemoryEquipmentReferenceRepository().asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository(),
                new HobbyAttributeTemplateService(
                        new AuthenticatedUserExtractor(),
                        hobbies.asRepository(),
                        userHobbies.asRepository(),
                        templateRepository.asRepository()
                ),
                new PlaceResolutionService(new InMemoryPlaceReferenceRepository().asRepository(), placeId -> {
                    throw new UnsupportedOperationException("No place resolution expected.");
                }),
                enabledFeatureFlags()
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
                Map.of("pages_read", 12)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void shouldRejectAttributeTypeMismatch() {
        InMemorySessionRecordRepository sessions = new InMemorySessionRecordRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryHobbyAttributeTemplateRepository templateRepository = new InMemoryHobbyAttributeTemplateRepository();

        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = hobbies.createHobby(hobbyId, "Running", "Sports & Movement");
        userHobbies.link("firebase-user-1", hobbyId);
        templateRepository.add(hobby, UUID.fromString("2f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "distance_km", "Distance", "number", "km", 1);

        SessionService service = new SessionService(
                new AuthenticatedUserExtractor(),
                sessions.asRepository(),
                hobbies.asRepository(),
                userHobbies.asRepository(),
                new InMemoryEquipmentReferenceRepository().asRepository(),
                new InMemoryBacklogItemReferenceRepository().asRepository(),
                new InMemoryPlaceReferenceRepository().asRepository(),
                new HobbyAttributeTemplateService(
                        new AuthenticatedUserExtractor(),
                        hobbies.asRepository(),
                        userHobbies.asRepository(),
                        templateRepository.asRepository()
                ),
                new PlaceResolutionService(new InMemoryPlaceReferenceRepository().asRepository(), placeId -> {
                    throw new UnsupportedOperationException("No place resolution expected.");
                }),
                enabledFeatureFlags()
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
                Map.of("distance_km", "far")
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid type");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static FeatureFlagService enabledFeatureFlags() {
        return new FeatureFlagService(new FeatureFlagProperties());
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
                        case "findAllByUserId" -> page(
                                storage.values().stream()
                                        .filter(item -> item.getUserId().equals(args[0]))
                                        .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                        .toList(),
                                (Pageable) args[1]
                        );
                        case "findAllByUserIdAndHobbyId" -> page(
                                storage.values().stream()
                                        .filter(item -> item.getUserId().equals(args[0]) && item.getHobby().getId().equals(args[1]))
                                        .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                        .toList(),
                                (Pageable) args[2]
                        );
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemorySessionRecordRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private PageImpl<SessionRecord> page(List<SessionRecord> items, Pageable pageable) {
            int fromIndex = Math.min((int) pageable.getOffset(), items.size());
            int toIndex = Math.min(fromIndex + pageable.getPageSize(), items.size());
            return new PageImpl<>(items.subList(fromIndex, toIndex), pageable, items.size());
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
                        case "findByIdAndUserId" -> Optional.ofNullable(storage.get((UUID) args[0]))
                                .filter(item -> item.getUserId().equals(args[1]));
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryBacklogItemReferenceRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryPlaceReferenceRepository {
        private final Map<String, PlaceReference> storage = new HashMap<>();

        void add(String placeId) {
            storage.put(placeId, new PlaceReference(
                    placeId,
                    OffsetDateTime.parse("2026-07-19T00:00:00Z")
            ));
        }

        PlaceReferenceRepository asRepository() {
            return (PlaceReferenceRepository) Proxy.newProxyInstance(
                    PlaceReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{PlaceReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsById" -> storage.containsKey(args[0]);
                        case "findById" -> Optional.ofNullable(storage.get(args[0]));
                        case "save" -> {
                            PlaceReference place = (PlaceReference) args[0];
                            storage.put(place.getPlaceId(), place);
                            yield place;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryPlaceReferenceRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryHobbyAttributeTemplateRepository {
        private final List<HobbyAttributeTemplate> storage = new ArrayList<>();

        void add(Hobby hobby, UUID id, String key, String label, String type, String unit, int displayOrder) {
            storage.add(ReflectionFactory.createTemplate(id, hobby, key, label, type, unit, displayOrder));
        }

        HobbyAttributeTemplateRepository asRepository() {
            return (HobbyAttributeTemplateRepository) Proxy.newProxyInstance(
                    HobbyAttributeTemplateRepository.class.getClassLoader(),
                    new Class<?>[]{HobbyAttributeTemplateRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByHobbyIdOrderByDisplayOrderAsc" -> storage.stream()
                                .filter(item -> item.getHobby().getId().equals(args[0]))
                                .sorted(Comparator.comparing(HobbyAttributeTemplate::getDisplayOrder))
                                .toList();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryHobbyAttributeTemplateRepository";
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

        static HobbyAttributeTemplate createTemplate(UUID id, Hobby hobby, String key, String label, String type, String unit, int displayOrder) {
            try {
                var constructor = HobbyAttributeTemplate.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                HobbyAttributeTemplate template = constructor.newInstance();
                setField(HobbyAttributeTemplate.class, template, "id", id);
                setField(HobbyAttributeTemplate.class, template, "hobby", hobby);
                setField(HobbyAttributeTemplate.class, template, "key", key);
                setField(HobbyAttributeTemplate.class, template, "label", label);
                setField(HobbyAttributeTemplate.class, template, "type", type);
                setField(HobbyAttributeTemplate.class, template, "unit", unit);
                setField(HobbyAttributeTemplate.class, template, "displayOrder", displayOrder);
                return template;
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
