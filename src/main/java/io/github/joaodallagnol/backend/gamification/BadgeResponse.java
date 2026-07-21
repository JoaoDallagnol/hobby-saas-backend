package io.github.joaodallagnol.backend.gamification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BadgeResponse(
        UUID id,
        String key,
        String name,
        String description,
        UUID hobbyId,
        String hobbyName,
        OffsetDateTime earnedAt
) {
}
