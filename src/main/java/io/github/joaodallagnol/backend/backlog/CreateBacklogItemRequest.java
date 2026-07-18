package io.github.joaodallagnol.backend.backlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateBacklogItemRequest(
        UUID hobbyId,
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @NotNull(message = "status is required")
        String status
) {
}
