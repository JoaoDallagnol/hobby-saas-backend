package io.github.joaodallagnol.backend.backlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.time.LocalDate;

public record UpdateBacklogItemRequest(
        UUID hobbyId,
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @NotNull(message = "status is required")
        String status,
        LocalDate dueDate,
        String priority,
        Boolean archived,
        Integer position
) {
    public UpdateBacklogItemRequest(UUID hobbyId, String title, String status) {
        this(hobbyId, title, status, null, null, null, null);
    }
}
