package io.github.joaodallagnol.backend.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AddUserHobbyRequest(
        @NotNull
        UUID hobbyId,
        @Size(max = 50)
        String experienceLevel
) {
}
