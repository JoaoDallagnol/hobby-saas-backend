package io.github.joaodallagnol.backend.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionPhotoRequest(
        @NotBlank
        @Size(max = 500)
        String storageKey
) {
}
