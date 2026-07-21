package io.github.joaodallagnol.backend.gamification;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class HobbyXpId implements Serializable {
    private String userId;
    private UUID hobbyId;

    public HobbyXpId() {
    }

    public HobbyXpId(String userId, UUID hobbyId) {
        this.userId = userId;
        this.hobbyId = hobbyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HobbyXpId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(hobbyId, that.hobbyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, hobbyId);
    }
}
