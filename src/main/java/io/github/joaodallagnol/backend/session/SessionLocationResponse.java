package io.github.joaodallagnol.backend.session;

public record SessionLocationResponse(
        String placeId,
        String name,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng
) {
}
