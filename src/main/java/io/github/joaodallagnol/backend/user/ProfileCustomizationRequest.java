package io.github.joaodallagnol.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ProfileCustomizationRequest(
        @NotBlank @Size(max = 30) String theme,
        @Size(max = 3) List<UUID> featuredBadgeIds
) {
}
