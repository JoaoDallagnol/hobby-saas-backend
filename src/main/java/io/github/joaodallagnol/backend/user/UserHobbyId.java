package io.github.joaodallagnol.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserHobbyId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "hobby_id", nullable = false)
    private UUID hobbyId;

    protected UserHobbyId() {
    }

    public UserHobbyId(String userId, UUID hobbyId) {
        this.userId = userId;
        this.hobbyId = hobbyId;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getHobbyId() {
        return hobbyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserHobbyId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(hobbyId, that.hobbyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, hobbyId);
    }
}
