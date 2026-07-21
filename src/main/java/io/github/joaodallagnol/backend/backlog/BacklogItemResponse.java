package io.github.joaodallagnol.backend.backlog;

import io.github.joaodallagnol.backend.session.BacklogItemReference;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.LocalDate;

public record BacklogItemResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String title,
        String status,
        OffsetDateTime createdAt,
        LocalDate dueDate,
        String priority,
        boolean archived,
        int position
) {

    public BacklogItemResponse(UUID id, UUID hobbyId, String hobbyName, String title, String status,
                               OffsetDateTime createdAt) {
        this(id, hobbyId, hobbyName, title, status, createdAt, null, "normal", false, 0);
    }

    public static BacklogItemResponse from(BacklogItemReference item) {
        return new BacklogItemResponse(
                item.getId(),
                item.getHobby() == null ? null : item.getHobby().getId(),
                item.getHobby() == null ? null : item.getHobby().getName(),
                item.getTitle(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getDueDate(),
                item.getPriority(),
                item.isArchived(),
                item.getPosition()
        );
    }
}
