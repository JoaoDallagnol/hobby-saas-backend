package io.github.joaodallagnol.backend.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.session.EquipmentReference;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class EquipmentServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateListUpdateAndDeleteEquipment() {
        InMemoryEquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        UUID runningId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby running = hobbyRepository.create(runningId, "Running", "Sports & Movement", "figure.run");
        userHobbyRepository.link("firebase-user-1", runningId);
        EquipmentService service = new EquipmentService(
                new AuthenticatedUserExtractor(),
                equipmentRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        EquipmentResponse created = service.createEquipment(new CreateEquipmentRequest(
                running.getId(),
                "Shoes",
                "Nike Pegasus 41"
        ));
        List<EquipmentResponse> allEquipment = service.listEquipment(null);
        List<EquipmentResponse> runningEquipment = service.listEquipment(running.getId());
        EquipmentResponse updated = service.updateEquipment(
                created.id(),
                new UpdateEquipmentRequest(null, "Camera", "Sony A6700")
        );
        service.deleteEquipment(created.id());

        assertThat(created.hobbyId()).isEqualTo(running.getId());
        assertThat(allEquipment).hasSize(1);
        assertThat(runningEquipment).hasSize(1);
        assertThat(updated.hobbyId()).isNull();
        assertThat(updated.category()).isEqualTo("Camera");
        assertThat(updated.name()).isEqualTo("Sony A6700");
        assertThat(service.listEquipment(null)).isEmpty();
    }

    @Test
    void shouldRejectEquipmentDeletionWhenLinkedToSessionHistory() {
        InMemoryEquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        EquipmentReference equipment = equipmentRepository.create("firebase-user-1", null, "Shoes", "Nike Pegasus 41");
        equipmentRepository.markAsUsed("firebase-user-1", equipment.getId());
        EquipmentService service = new EquipmentService(
                new AuthenticatedUserExtractor(),
                equipmentRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.deleteEquipment(equipment.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked to one or more sessions");
    }

    @Test
    void shouldRejectEquipmentHobbyOutsideUserProfile() {
        InMemoryEquipmentRepository equipmentRepository = new InMemoryEquipmentRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        UUID readingId = UUID.fromString("2f1f49ea-6b5d-4c2e-9ce7-3e621f081002");
        hobbyRepository.create(readingId, "Reading", "Mind & Study", "book");
        EquipmentService service = new EquipmentService(
                new AuthenticatedUserExtractor(),
                equipmentRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.createEquipment(new CreateEquipmentRequest(
                readingId,
                "Bookshelf",
                "Living Room Shelf"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not linked to the user profile");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class InMemoryEquipmentRepository {
        private final Map<UUID, EquipmentReference> storage = new HashMap<>();
        private final Set<String> usedKeys = new HashSet<>();

        EquipmentReference create(String userId, Hobby hobby, String category, String name) {
            EquipmentReference equipment = new EquipmentReference(userId, hobby, category, name);
            storage.put(equipment.getId(), equipment);
            return equipment;
        }

        void markAsUsed(String userId, UUID equipmentId) {
            usedKeys.add(userId + ":" + equipmentId);
        }

        EquipmentReferenceRepository asRepository() {
            return (EquipmentReferenceRepository) Proxy.newProxyInstance(
                    EquipmentReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{EquipmentReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> {
                            EquipmentReference equipment = (EquipmentReference) args[0];
                            storage.put(equipment.getId(), equipment);
                            yield equipment;
                        }
                        case "findAllByUserIdOrderByCategoryAscNameAsc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]))
                                .sorted(Comparator.comparing(EquipmentReference::getCategory)
                                        .thenComparing(EquipmentReference::getName))
                                .toList();
                        case "findAllByUserIdAndHobbyIdOrderByCategoryAscNameAsc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]))
                                .filter(item -> item.getHobby() != null && item.getHobby().getId().equals(args[1]))
                                .sorted(Comparator.comparing(EquipmentReference::getCategory)
                                        .thenComparing(EquipmentReference::getName))
                                .toList();
                        case "findByIdAndUserId" -> Optional.ofNullable(storage.get((UUID) args[0]))
                                .filter(item -> item.getUserId().equals(args[1]));
                        case "existsSessionUsageByUserIdAndEquipmentId" -> usedKeys.contains(args[0] + ":" + args[1]);
                        case "delete" -> {
                            EquipmentReference equipment = (EquipmentReference) args[0];
                            storage.remove(equipment.getId());
                            yield null;
                        }
                        case "findAllByIdInAndUserId" -> storage.values().stream()
                                .filter(item -> ((Set<UUID>) args[0]).contains(item.getId()))
                                .filter(item -> item.getUserId().equals(args[1]))
                                .toList();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryEquipmentRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class InMemoryHobbyRepository {
        private final Map<UUID, Hobby> storage = new HashMap<>();

        Hobby create(UUID id, String hobbyName, String categoryName, String icon) {
            HobbyCategory category = ReflectionFactory.createHobbyCategory(UUID.randomUUID(), categoryName);
            Hobby hobby = ReflectionFactory.createHobby(id, hobbyName, icon, category);
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
        private final List<String> links = new ArrayList<>();

        void link(String userId, UUID hobbyId) {
            links.add(userId + ":" + hobbyId);
        }

        UserHobbyRepository asRepository() {
            return (UserHobbyRepository) Proxy.newProxyInstance(
                    UserHobbyRepository.class.getClassLoader(),
                    new Class<?>[]{UserHobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByIdUserIdAndIdHobbyId" -> links.contains(args[0] + ":" + args[1]);
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryUserHobbyRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static final class ReflectionFactory {
        static HobbyCategory createHobbyCategory(UUID id, String name) {
            try {
                Constructor<HobbyCategory> constructor = HobbyCategory.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                HobbyCategory category = constructor.newInstance();
                setField(HobbyCategory.class, category, "id", id);
                setField(HobbyCategory.class, category, "name", name);
                return category;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static Hobby createHobby(UUID id, String name, String icon, HobbyCategory category) {
            try {
                Constructor<Hobby> constructor = Hobby.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                Hobby hobby = constructor.newInstance();
                setField(Hobby.class, hobby, "id", id);
                setField(Hobby.class, hobby, "name", name);
                setField(Hobby.class, hobby, "icon", icon);
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
