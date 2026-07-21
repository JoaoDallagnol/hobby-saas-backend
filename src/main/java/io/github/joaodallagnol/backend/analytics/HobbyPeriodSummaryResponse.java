package io.github.joaodallagnol.backend.analytics;

import java.util.UUID;

public record HobbyPeriodSummaryResponse(
        UUID hobbyId,
        String hobbyName,
        long sessions,
        long minutes
) {
}
