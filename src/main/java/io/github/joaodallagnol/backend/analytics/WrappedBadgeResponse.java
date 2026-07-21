package io.github.joaodallagnol.backend.analytics;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WrappedBadgeResponse(
        String key,
        UUID hobbyId,
        OffsetDateTime earnedAt
) {
}
