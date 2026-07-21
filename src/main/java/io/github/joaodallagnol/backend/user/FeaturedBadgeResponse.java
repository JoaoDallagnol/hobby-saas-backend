package io.github.joaodallagnol.backend.user;

import java.util.UUID;

public record FeaturedBadgeResponse(
        UUID id,
        String key,
        UUID hobbyId,
        int position
) {
}
