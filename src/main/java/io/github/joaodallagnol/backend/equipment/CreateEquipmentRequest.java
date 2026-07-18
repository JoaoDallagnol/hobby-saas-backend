package io.github.joaodallagnol.backend.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateEquipmentRequest(
        UUID hobbyId,
        @NotBlank(message = "category is required")
        @Size(max = 100, message = "category must be at most 100 characters")
        String category,
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name
) {
}
