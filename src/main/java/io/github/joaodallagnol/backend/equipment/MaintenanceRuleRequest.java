package io.github.joaodallagnol.backend.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceRuleRequest(
        UUID equipmentId,
        @NotBlank @Size(max = 120) String name,
        @Positive int intervalMinutes,
        OffsetDateTime lastMaintainedAt,
        Boolean active
) {
}
