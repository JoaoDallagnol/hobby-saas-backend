package io.github.joaodallagnol.backend.gamification;

import java.io.Serializable;
import java.util.Objects;

public class UserFeaturedBadgeId implements Serializable {
    private String userId;
    private int position;

    public UserFeaturedBadgeId() {
    }

    public UserFeaturedBadgeId(String userId, int position) {
        this.userId = userId;
        this.position = position;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserFeaturedBadgeId that)) return false;
        return position == that.position && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, position); }
}
