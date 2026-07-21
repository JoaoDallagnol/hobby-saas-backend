package io.github.joaodallagnol.backend.gamification;

import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String name,
        String metric,
        int targetValue,
        String cadence,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        boolean advanced,
        long progress,
        int progressPercent,
        boolean achieved
) {
}
