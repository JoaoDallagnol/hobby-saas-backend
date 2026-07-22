package io.github.joaodallagnol.backend.export;

import io.github.joaodallagnol.backend.session.SessionRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExportSessionResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String title,
        OffsetDateTime startedAt,
        int durationMinutes,
        String notes,
        int satisfaction,
        String visibility,
        String locationLabel,
        UUID projectId,
        List<UUID> equipmentIds,
        Map<String, Object> attributes,
        boolean hasPhoto
) {
    public static ExportSessionResponse from(SessionRecord session) {
        return new ExportSessionResponse(session.getId(), session.getHobby().getId(), session.getHobby().getName(),
                session.getTitle(), session.getStartedAt(), session.getDurationMinutes(), session.getNotes(),
                session.getSatisfaction(), session.getVisibility().value(),
                session.getLocationLabel(), session.getProjectId(),
                session.getEquipment().stream().map(item -> item.getId()).toList(), session.getAttributes(),
                !session.getPhotos().isEmpty());
    }
}
