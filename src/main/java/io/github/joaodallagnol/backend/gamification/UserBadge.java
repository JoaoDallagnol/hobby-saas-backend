package io.github.joaodallagnol.backend.gamification;

import io.github.joaodallagnol.backend.user.Hobby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_badges")
public class UserBadge {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "badge_key", nullable = false, length = 60)
    private String badgeKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;

    protected UserBadge() {
    }

    public UserBadge(String userId, String badgeKey, Hobby hobby, OffsetDateTime earnedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.badgeKey = badgeKey;
        this.hobby = hobby;
        this.earnedAt = earnedAt;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getBadgeKey() { return badgeKey; }
    public Hobby getHobby() { return hobby; }
    public OffsetDateTime getEarnedAt() { return earnedAt; }
}
