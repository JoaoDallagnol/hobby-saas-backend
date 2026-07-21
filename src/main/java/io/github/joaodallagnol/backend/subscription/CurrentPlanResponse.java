package io.github.joaodallagnol.backend.subscription;

import java.time.OffsetDateTime;

public record CurrentPlanResponse(
        String plan,
        boolean active,
        OffsetDateTime currentPeriodEnd
) {
}
