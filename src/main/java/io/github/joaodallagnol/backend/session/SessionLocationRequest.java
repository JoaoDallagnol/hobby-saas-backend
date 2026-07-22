package io.github.joaodallagnol.backend.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionLocationRequest(
        @NotBlank
        @Size(max = 255)
        String placeId,
        @NotBlank
        @Size(max = 150)
        String label
) {
}
