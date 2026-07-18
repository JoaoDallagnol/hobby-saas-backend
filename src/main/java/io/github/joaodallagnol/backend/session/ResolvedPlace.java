package io.github.joaodallagnol.backend.session;

import java.math.BigDecimal;

public record ResolvedPlace(
        String placeId,
        String name,
        BigDecimal lat,
        BigDecimal lng
) {
}
