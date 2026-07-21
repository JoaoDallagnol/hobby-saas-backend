package io.github.joaodallagnol.backend.session;

import java.util.UUID;

public record SessionPhotoResponse(
        UUID id,
        String originalUrl,
        String thumbnailUrl,
        String processingStatus,
        String deliveryStatus
) {
    public SessionPhotoResponse(UUID id, String originalUrl, String thumbnailUrl, String processingStatus) {
        this(id, originalUrl, thumbnailUrl, processingStatus, "ready");
    }
}
