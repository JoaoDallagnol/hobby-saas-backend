package io.github.joaodallagnol.backend.analytics;

import java.time.LocalDate;
import java.util.List;

public record PeriodSummaryResponse(
        LocalDate from,
        LocalDate to,
        long sessions,
        long minutes,
        long activeDays,
        int longestSessionMinutes,
        int bestStreakDays,
        List<HobbyPeriodSummaryResponse> hobbies,
        List<DailyActivityResponse> dailyActivity
) {
}
