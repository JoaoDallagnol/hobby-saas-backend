package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionPhotoMediaServiceTest {

    private final SessionPhotoMediaService mediaService = new SessionPhotoMediaService(
            storageKey -> "https://private.example.test/" + storageKey,
            "https://public.example.test"
    );

    @Test
    void shouldExposePendingPhotoAsProcessingWithoutLeakingTemporaryKey() {
        SessionPhoto photo = new SessionPhoto(null, "uploads/users/abc/session-temp/photo.jpg");

        SessionPhotoResponse response = mediaService.toOwnerResponse(photo);

        assertThat(response.processingStatus()).isEqualTo("pending");
        assertThat(response.deliveryStatus()).isEqualTo("processing");
        assertThat(response.originalUrl()).isNull();
        assertThat(response.thumbnailUrl()).isNull();
    }

    @Test
    void shouldExposeFailedPhotoAsUnavailableInsteadOfProcessingForever() {
        SessionPhoto photo = new SessionPhoto(null, "uploads/users/abc/session-temp/photo.jpg");
        photo.registerProcessingFailure("ImageProcessingException");
        photo.registerProcessingFailure("ImageProcessingException");
        photo.registerProcessingFailure("ImageProcessingException");

        SessionPhotoResponse response = mediaService.toOwnerResponse(photo);

        assertThat(response.processingStatus()).isEqualTo("failed");
        assertThat(response.deliveryStatus()).isEqualTo("unavailable");
        assertThat(response.originalUrl()).isNull();
        assertThat(response.thumbnailUrl()).isNull();
    }
}
