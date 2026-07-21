package io.github.joaodallagnol.backend.session;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String title,
        OffsetDateTime startedAt,
        int durationMinutes,
        String notes,
        int satisfaction,
        SessionVisibility visibility,
        SessionLocationResponse location,
        UUID projectId,
        List<UUID> equipmentIds,
        List<SessionPhotoResponse> photos,
        Map<String, Object> attributes
) {
    public SessionResponse(UUID id, UUID hobbyId, String hobbyName, String title, OffsetDateTime startedAt,
                           int durationMinutes, String notes, int satisfaction, SessionLocationResponse location,
                           UUID projectId, List<UUID> equipmentIds, List<SessionPhotoResponse> photos,
                           Map<String, Object> attributes) {
        this(id, hobbyId, hobbyName, title, startedAt, durationMinutes, notes, satisfaction,
                SessionVisibility.ONLY_ME, location, projectId, equipmentIds, photos, attributes);
    }

    public static SessionResponse from(SessionRecord session, SessionPhotoMediaService mediaService) {
        return new SessionResponse(
                session.getId(),
                session.getHobby().getId(),
                session.getHobby().getName(),
                session.getTitle(),
                session.getStartedAt(),
                session.getDurationMinutes(),
                session.getNotes(),
                session.getSatisfaction(),
                session.getVisibility(),
                session.getPlaceId() == null ? null : new SessionLocationResponse(
                        session.getPlaceId(),
                        session.getPlace() == null ? null : session.getPlace().getName(),
                        session.getPlace() == null ? null : session.getPlace().getLat(),
                        session.getPlace() == null ? null : session.getPlace().getLng()
                ),
                session.getProjectId(),
                session.getEquipment().stream().map(EquipmentReference::getId).toList(),
                session.getPhotos().stream().map(mediaService::toOwnerResponse).toList(),
                session.getAttributes()
        );
    }
}
