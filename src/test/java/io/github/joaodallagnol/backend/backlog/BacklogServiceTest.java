package io.github.joaodallagnol.backend.backlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.session.BacklogItemReference;
import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
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

class BacklogServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateListUpdateAndDeleteBacklogItem() {
        InMemoryBacklogRepository backlogRepository = new InMemoryBacklogRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        UUID runningId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby running = hobbyRepository.create(runningId, "Running", "Sports & Movement", "figure.run");
        userHobbyRepository.link("firebase-user-1", runningId);
        BacklogService service = new BacklogService(
                new AuthenticatedUserExtractor(),
                backlogRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        BacklogItemResponse created = service.createItem(new CreateBacklogItemRequest(
                running.getId(),
                "10k race plan",
                "pending"
        ));
        List<BacklogItemResponse> allItems = service.listItems(null);
        List<BacklogItemResponse> runningItems = service.listItems(running.getId());
        BacklogItemResponse updated = service.updateItem(
                created.id(),
                new UpdateBacklogItemRequest(running.getId(), "Half marathon plan", "in_progress")
        );
        service.deleteItem(created.id());

        assertThat(created.hobbyId()).isEqualTo(running.getId());
        assertThat(created.status()).isEqualTo("pending");
        assertThat(allItems).hasSize(1);
        assertThat(runningItems).hasSize(1);
        assertThat(updated.title()).isEqualTo("Half marathon plan");
        assertThat(updated.status()).isEqualTo("in_progress");
        assertThat(service.listItems(null)).isEmpty();
    }

    @Test
    void shouldRejectBacklogDeletionWhenLinkedToSessionHistory() {
        InMemoryBacklogRepository backlogRepository = new InMemoryBacklogRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        BacklogItemReference item = backlogRepository.create("firebase-user-1", null, "Project shelf", "pending");
        backlogRepository.markAsUsed("firebase-user-1", item.getId());
        BacklogService service = new BacklogService(
                new AuthenticatedUserExtractor(),
                backlogRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.deleteItem(item.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked to one or more sessions");
    }

    @Test
    void shouldRejectInvalidStatus() {
        InMemoryBacklogRepository backlogRepository = new InMemoryBacklogRepository();
        InMemoryHobbyRepository hobbyRepository = new InMemoryHobbyRepository();
        InMemoryUserHobbyRepository userHobbyRepository = new InMemoryUserHobbyRepository();
        BacklogService service = new BacklogService(
                new AuthenticatedUserExtractor(),
                backlogRepository.asRepository(),
                hobbyRepository.asRepository(),
                userHobbyRepository.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.createItem(new CreateBacklogItemRequest(
                null,
                "Build camera rig",
                "todo"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid backlog status");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class InMemoryBacklogRepository {
        private final Map<UUID, BacklogItemReference> storage = new HashMap<>();
        private final Set<String> usedKeys = new HashSet<>();

        BacklogItemReference create(String userId, Hobby hobby, String title, String status) {
            BacklogItemReference item = new BacklogItemReference(userId, hobby, title, status);
            storage.put(item.getId(), item);
            return item;
        }

        void markAsUsed(String userId, UUID itemId) {
            usedKeys.add(userId + ":" + itemId);
        }

        BacklogItemReferenceRepository asRepository() {
            return (BacklogItemReferenceRepository) Proxy.newProxyInstance(
                    BacklogItemReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{BacklogItemReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> {
                            BacklogItemReference item = (BacklogItemReference) args[0];
                            storage.put(item.getId(), item);
                            yield item;
                        }
                        case "existsByIdAndUserId" -> storage.values().stream()
                                .anyMatch(item -> item.getId().equals(args[0]) && item.getUserId().equals(args[1]));
                        case "findAllByUserIdOrderByCreatedAtDesc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]))
                                .sorted(Comparator.comparing(BacklogItemReference::getCreatedAt).reversed())
                                .toList();
                        case "findAllByUserIdAndHobbyIdOrderByCreatedAtDesc" -> storage.values().stream()
                                .filter(item -> item.getUserId().equals(args[0]))
                                .filter(item -> item.getHobby() != null && item.getHobby().getId().equals(args[1]))
                                .sorted(Comparator.comparing(BacklogItemReference::getCreatedAt).reversed())
                                .toList();
                        case "findByIdAndUserId" -> Optional.ofNullable(storage.get((UUID) args[0]))
                                .filter(item -> item.getUserId().equals(args[1]));
                        case "existsSessionUsageByUserIdAndProjectId" -> usedKeys.contains(args[0] + ":" + args[1]);
                        case "delete" -> {
                            BacklogItemReference item = (BacklogItemReference) args[0];
                            storage.remove(item.getId());
                            yield null;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryBacklogRepository";
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
