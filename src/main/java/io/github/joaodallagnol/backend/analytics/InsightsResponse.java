package io.github.joaodallagnol.backend.analytics;

public record InsightsResponse(
        PeriodSummaryResponse current,
        PeriodSummaryResponse previous,
        long sessionsChange,
        long minutesChange,
        long activeDaysChange
) {
}
