package io.github.joaodallagnol.backend.analytics;

import java.time.LocalDate;

public record DailyActivityResponse(
        LocalDate date,
        long sessions,
        long minutes
) {
}
