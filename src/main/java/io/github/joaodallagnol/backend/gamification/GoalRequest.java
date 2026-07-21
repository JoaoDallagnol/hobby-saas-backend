package io.github.joaodallagnol.backend.gamification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record GoalRequest(
        UUID hobbyId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank String metric,
        @Positive int targetValue,
        @NotBlank String cadence,
        LocalDate startDate,
        LocalDate endDate
) {
}
