package io.github.joaodallagnol.backend.gamification;

import java.util.UUID;

public record HobbyRecordResponse(UUID hobbyId, String hobbyName, long value) {
}
