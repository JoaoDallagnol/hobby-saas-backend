package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SessionPhotoUploadServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGeneratePresignedUploadForSupportedImage() {
        authenticate("firebase-user-1", "user@example.com", "User");
        SessionPhotoUploadService service = new SessionPhotoUploadService(
                new AuthenticatedUserExtractor(),
                (storageKey, contentType) -> new GeneratedUploadUrl(
                        "https://example.r2.dev/upload",
                        "PUT",
                        Map.of("Content-Type", contentType),
                        OffsetDateTime.parse("2026-07-18T20:00:00Z")
                )
        );

        SessionPhotoUploadResponse response = service.createUpload(new CreateSessionPhotoUploadRequest(
                "image/jpeg",
                "run.jpg",
                512_000L
        ));

        assertThat(response.storageKey()).startsWith("uploads/firebase-user-1/session-temp/");
        assertThat(response.storageKey()).endsWith(".jpg");
        assertThat(response.uploadUrl()).isEqualTo("https://example.r2.dev/upload");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", "image/jpeg");
    }

    @Test
    void shouldRejectUnsupportedImageType() {
        authenticate("firebase-user-1", "user@example.com", "User");
        SessionPhotoUploadService service = new SessionPhotoUploadService(
                new AuthenticatedUserExtractor(),
                (storageKey, contentType) -> new GeneratedUploadUrl("", "PUT", Map.of(), OffsetDateTime.now())
        );

        assertThatThrownBy(() -> service.createUpload(new CreateSessionPhotoUploadRequest(
                "application/pdf",
                "file.pdf",
                1000L
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image content type");
    }

    @Test
    void shouldRejectOversizedImage() {
        authenticate("firebase-user-1", "user@example.com", "User");
        SessionPhotoUploadService service = new SessionPhotoUploadService(
                new AuthenticatedUserExtractor(),
                (storageKey, contentType) -> new GeneratedUploadUrl("", "PUT", Map.of(), OffsetDateTime.now())
        );

        assertThatThrownBy(() -> service.createUpload(new CreateSessionPhotoUploadRequest(
                "image/png",
                "big.png",
                11L * 1024 * 1024
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed size");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }
}
