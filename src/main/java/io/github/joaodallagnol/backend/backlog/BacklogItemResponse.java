package io.github.joaodallagnol.backend.backlog;

import io.github.joaodallagnol.backend.session.BacklogItemReference;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BacklogItemResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String title,
        String status,
        OffsetDateTime createdAt
) {

    public static BacklogItemResponse from(BacklogItemReference item) {
        return new BacklogItemResponse(
                item.getId(),
                item.getHobby() == null ? null : item.getHobby().getId(),
                item.getHobby() == null ? null : item.getHobby().getName(),
                item.getTitle(),
                item.getStatus(),
                item.getCreatedAt()
        );
    }
}
