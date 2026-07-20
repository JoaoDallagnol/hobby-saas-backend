package io.github.joaodallagnol.backend.session;

import java.util.UUID;

public record SessionPhotoResponse(
        UUID id,
        String storageKeyOriginal,
        String storageKeyThumbnail,
        String processingStatus
) {
    public static SessionPhotoResponse from(SessionPhoto photo) {
        return new SessionPhotoResponse(
                photo.getId(),
                photo.getStorageKeyOriginal(),
                photo.getStorageKeyThumbnail(),
                photo.getProcessingStatus()
        );
    }
}
