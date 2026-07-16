package io.github.joaodallagnol.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthenticatedUserExtractorTest {

    private final AuthenticatedUserExtractor extractor = new AuthenticatedUserExtractor();

    @Test
    void shouldExtractAuthenticatedUserFromJwtClaims() {
        Jwt jwt = jwt(Map.of(
                "sub", "e1b7ac0b-dc72-4dbf-b6a8-a1a2ef1d93e6",
                "email", "user@example.com",
                "name", "Example User",
                "email_verified", true
        ));

        AuthenticatedUser authenticatedUser = extractor.extract(jwt);

        assertThat(authenticatedUser.email()).isEqualTo("user@example.com");
        assertThat(authenticatedUser.name()).isEqualTo("Example User");
        assertThat(authenticatedUser.emailVerified()).isTrue();
    }

    @Test
    void shouldRejectJwtWithoutEmail() {
        Jwt jwt = jwt(Map.of(
                "sub", "e1b7ac0b-dc72-4dbf-b6a8-a1a2ef1d93e6",
                "name", "Example User"
        ));

        assertThatThrownBy(() -> extractor.extract(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldFallbackToPreferredUsernameWhenNameIsMissing() {
        Jwt jwt = jwt(Map.of(
                "sub", "e1b7ac0b-dc72-4dbf-b6a8-a1a2ef1d93e6",
                "email", "user@example.com",
                "preferred_username", "example-user"
        ));

        AuthenticatedUser authenticatedUser = extractor.extract(jwt);

        assertThat(authenticatedUser.name()).isEqualTo("example-user");
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }
}
