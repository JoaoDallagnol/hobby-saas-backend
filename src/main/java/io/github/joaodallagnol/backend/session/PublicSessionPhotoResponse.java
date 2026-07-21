package io.github.joaodallagnol.backend.session;

import java.util.UUID;

public record PublicSessionPhotoResponse(
        UUID id,
        String originalUrl,
        String thumbnailUrl,
        String processingStatus,
        String deliveryStatus
) {
    static PublicSessionPhotoResponse from(SessionPhoto photo, SessionPhotoMediaService mediaService) {
        SessionPhotoResponse media = mediaService.toPublicResponse(photo);
        return new PublicSessionPhotoResponse(media.id(), media.originalUrl(), media.thumbnailUrl(),
                media.processingStatus(), media.deliveryStatus());
    }
}
