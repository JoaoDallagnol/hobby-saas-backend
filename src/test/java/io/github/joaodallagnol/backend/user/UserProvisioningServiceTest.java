package io.github.joaodallagnol.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private ProductUserRepository productUserRepository;

    @InjectMocks
    private UserProvisioningService userProvisioningService;

    @Test
    void shouldCreateUserWhenItDoesNotExist() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "user@example.com", "Example User", true);

        when(productUserRepository.findById(userId)).thenReturn(Optional.empty());
        when(productUserRepository.save(any(ProductUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUser productUser = userProvisioningService.provisionIfMissing(authenticatedUser);

        assertThat(productUser.getId()).isEqualTo(userId);
        assertThat(productUser.getEmail()).isEqualTo("user@example.com");
        assertThat(productUser.getName()).isEqualTo("Example User");
        assertThat(productUser.isEmailVerified()).isTrue();
        assertThat(productUser.getBio()).isNull();
        assertThat(productUser.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        verify(productUserRepository).save(any(ProductUser.class));
    }

    @Test
    void shouldReuseExistingUserWhenAlreadyProvisioned() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "user@example.com", "Example User", true);
        ProductUser existingUser = new ProductUser(
                userId,
                "user@example.com",
                "Example User",
                true,
                "bio",
                OffsetDateTime.now().minusDays(1)
        );

        when(productUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ProductUser productUser = userProvisioningService.provisionIfMissing(authenticatedUser);

        assertThat(productUser).isSameAs(existingUser);
    }
}
