package io.github.joaodallagnol.backend.gamification;

import java.util.UUID;

public record HobbyProgressResponse(
        UUID hobbyId,
        String hobbyName,
        int xp,
        int level,
        String levelLabel,
        Integer nextLevelXp
) {
}
