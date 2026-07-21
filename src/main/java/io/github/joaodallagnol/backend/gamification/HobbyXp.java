package io.github.joaodallagnol.backend.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hobby_xp")
@IdClass(HobbyXpId.class)
public class HobbyXp {

    @Id
    @Column(name = "user_id", length = 128)
    private String userId;

    @Id
    @Column(name = "hobby_id")
    private UUID hobbyId;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private int level;

    @Column(name = "level_label", nullable = false, length = 50)
    private String levelLabel;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected HobbyXp() {
    }

    public HobbyXp(String userId, UUID hobbyId, int xp, int level, String levelLabel, OffsetDateTime updatedAt) {
        this.userId = userId;
        this.hobbyId = hobbyId;
        update(xp, level, levelLabel, updatedAt);
    }

    public String getUserId() { return userId; }
    public UUID getHobbyId() { return hobbyId; }
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public String getLevelLabel() { return levelLabel; }

    public void update(int xp, int level, String levelLabel, OffsetDateTime updatedAt) {
        this.xp = xp;
        this.level = level;
        this.levelLabel = levelLabel;
        this.updatedAt = updatedAt;
    }
}
