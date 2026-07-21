package io.github.joaodallagnol.backend.analytics;

import java.util.List;

public record WrappedResponse(
        String period,
        PeriodSummaryResponse summary,
        PeriodSummaryResponse previousPeriod,
        List<WrappedBadgeResponse> badgesEarned
) {
}
