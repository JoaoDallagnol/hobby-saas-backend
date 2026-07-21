package io.github.joaodallagnol.backend.gamification;

import java.time.YearMonth;

public record MonthlyChallengeResponse(
        String key,
        String name,
        YearMonth month,
        String metric,
        int target,
        long progress,
        int progressPercent,
        boolean achieved
) {
}
