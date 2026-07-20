package io.github.joaodallagnol.backend.user;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductUserTest {

    @Test
    void shouldUpdateEditableProfileFieldsOnly() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-19T12:00:00Z");
        ProductUser user = new ProductUser(
                "firebase-user-1",
                "user@example.com",
                "Old Name",
                true,
                "Old bio",
                createdAt
        );

        user.updateProfile("New Name", "New bio");

        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getBio()).isEqualTo("New bio");
        assertThat(user.getId()).isEqualTo("firebase-user-1");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }
}
