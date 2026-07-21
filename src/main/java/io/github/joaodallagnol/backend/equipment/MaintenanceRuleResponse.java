package io.github.joaodallagnol.backend.equipment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceRuleResponse(
        UUID id,
        UUID equipmentId,
        String equipmentName,
        String name,
        int intervalMinutes,
        OffsetDateTime lastMaintainedAt,
        boolean active,
        long usedMinutes,
        long remainingMinutes,
        boolean maintenanceDue
) {
}
