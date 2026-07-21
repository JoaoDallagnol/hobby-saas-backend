package io.github.joaodallagnol.backend.user;

import java.util.List;

public record ProfileCustomizationResponse(
        String theme,
        boolean supporterBadge,
        List<FeaturedBadgeResponse> featuredBadges
) {
}
