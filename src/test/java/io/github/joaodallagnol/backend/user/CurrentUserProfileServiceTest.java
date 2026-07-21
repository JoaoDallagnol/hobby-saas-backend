package io.github.joaodallagnol.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProfileServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUpdateCurrentUserProfile() {
        InMemoryProductUserRepository users = new InMemoryProductUserRepository();
        ProductUser productUser = new ProductUser("firebase-user-1", "user@example.com", "Old Name", true, "old bio", OffsetDateTime.now());
        users.storage.put(productUser.getId(), productUser);
        CurrentUserProfileService service = new CurrentUserProfileService(
                new AuthenticatedUserExtractor(),
                users.asRepository(),
                new InMemoryUserHobbyRepository().asRepository(),
                new InMemoryHobbyRepository().asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "Old Name");

        CurrentUserProfileResponse response = service.updateCurrentUserProfile(new CurrentUserProfileUpdateRequest("New Name", "new bio"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.bio()).isEqualTo("new bio");
        assertThat(productUser.getName()).isEqualTo("New Name");
    }

    @Test
    void shouldSetUniqueUsernameAndRejectReservedOrDuplicateValues() {
        InMemoryProductUserRepository users = new InMemoryProductUserRepository();
        ProductUser productUser = new ProductUser("firebase-user-1", "user@example.com", "User", true, null, OffsetDateTime.now());
        ProductUser other = new ProductUser("firebase-user-2", "other@example.com", "Other", "already.used", true, null, OffsetDateTime.now());
        users.storage.put(productUser.getId(), productUser);
        users.storage.put(other.getId(), other);
        CurrentUserProfileService service = new CurrentUserProfileService(
                new AuthenticatedUserExtractor(), users.asRepository(),
                new InMemoryUserHobbyRepository().asRepository(), new InMemoryHobbyRepository().asRepository());
        authenticate("firebase-user-1", "user@example.com", "User");

        CurrentUserProfileResponse updated = service.updateCurrentUserProfile(
                new CurrentUserProfileUpdateRequest("User", null, "new.user"));

        assertThat(updated.username()).isEqualTo("new.user");
        assertThatThrownBy(() -> service.updateCurrentUserProfile(
                new CurrentUserProfileUpdateRequest("User", null, "already.used")))
                .isInstanceOf(UsernameAlreadyTakenException.class);
        assertThatThrownBy(() -> service.updateCurrentUserProfile(
                new CurrentUserProfileUpdateRequest("User", null, "admin")))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    @Test
    void shouldAddListUpdateAndRemoveUserHobby() {
        InMemoryProductUserRepository users = new InMemoryProductUserRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        ProductUser productUser = new ProductUser("firebase-user-1", "user@example.com", "User", true, null, OffsetDateTime.now());
        users.storage.put(productUser.getId(), productUser);
        Hobby hobby = hobbies.create(UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "Running", "Sports & Movement", "figure.run");
        CurrentUserProfileService service = new CurrentUserProfileService(
                new AuthenticatedUserExtractor(),
                users.asRepository(),
                userHobbies.asRepository(),
                hobbies.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        UserHobbyResponse created = service.addCurrentUserHobby(new AddUserHobbyRequest(hobby.getId(), "beginner"));
        List<UserHobbyResponse> hobbiesResponse = service.getCurrentUserHobbies();
        UserHobbyResponse updated = service.updateCurrentUserHobby(hobby.getId(), new UpdateUserHobbyRequest("intermediate"));
        service.removeCurrentUserHobby(hobby.getId());

        assertThat(created.hobbyName()).isEqualTo("Running");
        assertThat(hobbiesResponse).hasSize(1);
        assertThat(updated.experienceLevel()).isEqualTo("intermediate");
        assertThat(service.getCurrentUserHobbies()).isEmpty();
    }

    @Test
    void shouldRejectDuplicateUserHobby() {
        InMemoryProductUserRepository users = new InMemoryProductUserRepository();
        InMemoryUserHobbyRepository userHobbies = new InMemoryUserHobbyRepository();
        InMemoryHobbyRepository hobbies = new InMemoryHobbyRepository();
        ProductUser productUser = new ProductUser("firebase-user-1", "user@example.com", "User", true, null, OffsetDateTime.now());
        users.storage.put(productUser.getId(), productUser);
        Hobby hobby = hobbies.create(UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001"), "Running", "Sports & Movement", "figure.run");
        userHobbies.storage.add(new UserHobby(productUser, hobby, "beginner"));
        CurrentUserProfileService service = new CurrentUserProfileService(
                new AuthenticatedUserExtractor(),
                users.asRepository(),
                userHobbies.asRepository(),
                hobbies.asRepository()
        );
        authenticate("firebase-user-1", "user@example.com", "User");

        assertThatThrownBy(() -> service.addCurrentUserHobby(new AddUserHobbyRequest(hobby.getId(), "beginner")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class InMemoryProductUserRepository {
        private final Map<String, ProductUser> storage = new HashMap<>();

        ProductUserRepository asRepository() {
            return (ProductUserRepository) Proxy.newProxyInstance(
                    ProductUserRepository.class.getClassLoader(),
                    new Class<?>[]{ProductUserRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(storage.get((String) args[0]));
                        case "save" -> {
                            ProductUser user = (ProductUser) args[0];
                            storage.put(user.getId(), user);
                            yield user;
                        }
                        case "existsByUsernameIgnoreCaseAndIdNot" -> storage.values().stream()
                                .anyMatch(user -> user.getUsername() != null
                                        && user.getUsername().equalsIgnoreCase((String) args[0])
                                        && !user.getId().equals(args[1]));
                        case "flush" -> null;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryProductUserRepository";
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
        private final List<UserHobby> storage = new ArrayList<>();

        UserHobbyRepository asRepository() {
            return (UserHobbyRepository) Proxy.newProxyInstance(
                    UserHobbyRepository.class.getClassLoader(),
                    new Class<?>[]{UserHobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByIdUserIdOrderByHobbyNameAsc" -> storage.stream()
                                .filter(item -> item.getUser().getId().equals(args[0]))
                                .sorted(Comparator.comparing(item -> item.getHobby().getName()))
                                .toList();
                        case "findByIdUserIdAndIdHobbyId" -> storage.stream()
                                .filter(item -> item.getUser().getId().equals(args[0]) && item.getHobby().getId().equals(args[1]))
                                .findFirst();
                        case "existsByIdUserIdAndIdHobbyId" -> storage.stream()
                                .anyMatch(item -> item.getUser().getId().equals(args[0]) && item.getHobby().getId().equals(args[1]));
                        case "save" -> {
                            UserHobby userHobby = (UserHobby) args[0];
                            storage.removeIf(item -> item.getId().equals(userHobby.getId()));
                            storage.add(userHobby);
                            yield userHobby;
                        }
                        case "delete" -> {
                            storage.remove(args[0]);
                            yield null;
                        }
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
                HobbyCategory category = HobbyCategory.class.getDeclaredConstructor().newInstance();
                setField(HobbyCategory.class, category, "id", id);
                setField(HobbyCategory.class, category, "name", name);
                return category;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static Hobby createHobby(UUID id, String name, String icon, HobbyCategory category) {
            try {
                Hobby hobby = Hobby.class.getDeclaredConstructor().newInstance();
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
