package io.github.joaodallagnol.backend.user;

import java.util.UUID;

public record HobbySummaryResponse(
        UUID id,
        String name,
        String categoryName,
        String icon
) {
}
