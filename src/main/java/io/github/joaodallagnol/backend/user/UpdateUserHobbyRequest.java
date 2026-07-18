package io.github.joaodallagnol.backend.user;

import jakarta.validation.constraints.Size;

public record UpdateUserHobbyRequest(
        @Size(max = 50)
        String experienceLevel
) {
}
