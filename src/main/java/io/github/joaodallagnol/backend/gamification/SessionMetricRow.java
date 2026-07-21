package io.github.joaodallagnol.backend.gamification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionMetricRow(
        UUID hobbyId,
        String hobbyName,
        String categoryName,
        int xpSessionBonus,
        int xpMinutesPerPoint,
        int durationMinutes,
        OffsetDateTime startedAt
) {
}
