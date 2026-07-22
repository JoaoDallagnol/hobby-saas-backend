package io.github.joaodallagnol.backend.session;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicSessionResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String title,
        OffsetDateTime startedAt,
        int durationMinutes,
        String notes,
        int satisfaction,
        String locationLabel,
        List<PublicSessionPhotoResponse> photos,
        Map<String, Object> attributes
) {
    public static PublicSessionResponse from(SessionRecord session, SessionPhotoMediaService mediaService) {
        return new PublicSessionResponse(
                session.getId(),
                session.getHobby().getId(),
                session.getHobby().getName(),
                session.getTitle(),
                session.getStartedAt(),
                session.getDurationMinutes(),
                session.getNotes(),
                session.getSatisfaction(),
                session.getLocationLabel(),
                session.getPhotos().stream().map(photo -> PublicSessionPhotoResponse.from(photo, mediaService)).toList(),
                session.getAttributes()
        );
    }
}
