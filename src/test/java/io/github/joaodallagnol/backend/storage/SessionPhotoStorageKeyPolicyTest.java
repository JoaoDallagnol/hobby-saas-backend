package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionPhotoStorageKeyPolicyTest {

    @Test
    void shouldCreateFilesystemSafeUserSpecificPrefix() {
        String prefix = SessionPhotoStorageKeyPolicy.uploadPrefix("firebase/user+1");

        assertThat(prefix).startsWith("uploads/users/").endsWith("/session-temp/");
        assertThat(prefix).doesNotContain("firebase/user+1");
        assertThatCode(() -> SessionPhotoStorageKeyPolicy.requireOwnedUploadKey(
                "firebase/user+1",
                prefix + "2026/07/19/photo.webp"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectStorageKeyOwnedByAnotherUser() {
        String otherUserKey = SessionPhotoStorageKeyPolicy.uploadPrefix("other-user") + "photo.webp";

        assertThatThrownBy(() -> SessionPhotoStorageKeyPolicy.requireOwnedUploadKey("current-user", otherUserKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }
}
