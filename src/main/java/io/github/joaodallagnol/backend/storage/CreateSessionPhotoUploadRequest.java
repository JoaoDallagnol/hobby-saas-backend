package io.github.joaodallagnol.backend.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSessionPhotoUploadRequest(
        @NotBlank(message = "contentType is required")
        @Size(max = 100, message = "contentType must be at most 100 characters")
        String contentType,
        @Size(max = 255, message = "fileName must be at most 255 characters")
        String fileName,
        @NotNull(message = "sizeBytes is required")
        @Positive(message = "sizeBytes must be positive")
        Long sizeBytes
) {
}
