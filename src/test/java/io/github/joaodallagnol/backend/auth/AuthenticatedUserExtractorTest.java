package io.github.joaodallagnol.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuthenticatedUserExtractorTest {

    private final AuthenticatedUserExtractor extractor = new AuthenticatedUserExtractor();

    @Test
    void shouldExtractAuthenticatedUserFromFirebasePrincipal() {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(
                "firebase-user-123",
                "user@example.com",
                "Example User",
                true
        );

        AuthenticatedUser authenticatedUser = extractor.extract(new UsernamePasswordAuthenticationToken(principal, "token"));

        assertThat(authenticatedUser.id()).isEqualTo("firebase-user-123");
        assertThat(authenticatedUser.email()).isEqualTo("user@example.com");
        assertThat(authenticatedUser.name()).isEqualTo("Example User");
        assertThat(authenticatedUser.emailVerified()).isTrue();
    }

    @Test
    void shouldRejectPrincipalWithoutEmail() {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(
                "firebase-user-123",
                null,
                "Example User",
                true
        );

        assertThatThrownBy(() -> extractor.extract(new UsernamePasswordAuthenticationToken(principal, "token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldFallbackToEmailPrefixWhenNameIsMissing() {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(
                "firebase-user-123",
                "user@example.com",
                null,
                true
        );

        AuthenticatedUser authenticatedUser = extractor.extract(new UsernamePasswordAuthenticationToken(principal, "token"));

        assertThat(authenticatedUser.name()).isEqualTo("user");
    }
}
