package io.github.joaodallagnol.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserProvisioningServiceTest {

    @Test
    void shouldCreateUserWhenItDoesNotExist() {
        InMemoryUserRepository repositoryState = new InMemoryUserRepository();
        ProductUserRepository repository = repositoryState.asRepository();
        UserProvisioningService userProvisioningService = new UserProvisioningService(repository);

        String userId = "firebase-user-123";
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "user@example.com", "Example User", true);

        ProductUser productUser = userProvisioningService.provisionIfMissing(authenticatedUser);

        assertThat(productUser.getId()).isEqualTo(userId);
        assertThat(productUser.getEmail()).isEqualTo("user@example.com");
        assertThat(productUser.getName()).isEqualTo("Example User");
        assertThat(productUser.isEmailVerified()).isTrue();
        assertThat(productUser.getBio()).isNull();
        assertThat(productUser.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(repositoryState.storage()).containsKey(userId);
    }

    @Test
    void shouldReuseExistingUserWhenAlreadyProvisioned() {
        InMemoryUserRepository repositoryState = new InMemoryUserRepository();
        String userId = "firebase-user-456";
        ProductUser existingUser = new ProductUser(
                userId,
                "user@example.com",
                "Example User",
                true,
                "bio",
                OffsetDateTime.now().minusDays(1)
        );
        repositoryState.storage().put(userId, existingUser);
        ProductUserRepository repository = repositoryState.asRepository();
        UserProvisioningService userProvisioningService = new UserProvisioningService(repository);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "user@example.com", "Example User", true);

        ProductUser productUser = userProvisioningService.provisionIfMissing(authenticatedUser);

        assertThat(productUser).isSameAs(existingUser);
    }

    private static final class InMemoryUserRepository {

        private final Map<String, ProductUser> storage = new HashMap<>();

        Map<String, ProductUser> storage() {
            return storage;
        }

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
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryProductUserRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }
    }
}
