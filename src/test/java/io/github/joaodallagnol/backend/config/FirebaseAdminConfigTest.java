package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.LocalDevelopmentTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseAdminConfigTest {

    private final FirebaseAdminConfig config = new FirebaseAdminConfig();

    @Test
    void shouldUseLocalDevelopmentVerifierWhenEnabledInLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        FirebaseTokenVerifier verifier = config.firebaseTokenVerifier(
                environment,
                "",
                "",
                "",
                true,
                "local-dev-token",
                "local-user",
                "local@example.com",
                "Local User",
                true,
                "secondary-token",
                "secondary-user",
                "secondary@example.com",
                "Secondary User",
                true
        );

        assertThat(verifier).isInstanceOf(LocalDevelopmentTokenVerifier.class);
    }

    @Test
    void shouldRejectLocalDevelopmentVerifierOutsideLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.firebaseTokenVerifier(
                environment,
                "",
                "",
                "",
                true,
                "local-dev-token",
                "local-user",
                "local@example.com",
                "Local User",
                true,
                "",
                "secondary-user",
                "secondary@example.com",
                "Secondary User",
                true
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Local development auth can only be enabled with the local profile.");
    }
}
