package io.github.joaodallagnol.backend.streak;

import java.time.LocalDate;

public record StreakResponse(
        int currentStreakDays,
        LocalDate lastActiveDateUtc,
        LocalDate referenceDateUtc,
        boolean activeToday
) {
}
