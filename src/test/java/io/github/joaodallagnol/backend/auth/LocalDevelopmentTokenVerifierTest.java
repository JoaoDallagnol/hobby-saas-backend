package io.github.joaodallagnol.backend.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDevelopmentTokenVerifierTest {

    @Test
    void shouldReturnConfiguredUserWhenTokenMatches() {
        FirebaseVerifiedToken expectedUser = new FirebaseVerifiedToken(
                "local-user",
                "local@example.com",
                "Local User",
                true
        );
        LocalDevelopmentTokenVerifier verifier = new LocalDevelopmentTokenVerifier("local-dev-token", expectedUser);

        FirebaseVerifiedToken verifiedToken = verifier.verify("local-dev-token");

        assertThat(verifiedToken).isEqualTo(expectedUser);
    }

    @Test
    void shouldRejectUnexpectedToken() {
        LocalDevelopmentTokenVerifier verifier = new LocalDevelopmentTokenVerifier(
                "local-dev-token",
                new FirebaseVerifiedToken("local-user", "local@example.com", "Local User", true)
        );

        assertThatThrownBy(() -> verifier.verify("wrong-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token is invalid.");
    }
}
