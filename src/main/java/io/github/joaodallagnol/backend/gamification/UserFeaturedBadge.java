package io.github.joaodallagnol.backend.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_featured_badges")
@IdClass(UserFeaturedBadgeId.class)
public class UserFeaturedBadge {
    @Id
    @Column(name = "user_id", length = 128)
    private String userId;

    @Id
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private UserBadge badge;

    protected UserFeaturedBadge() {
    }

    public UserFeaturedBadge(String userId, int position, UserBadge badge) {
        this.userId = userId;
        this.position = position;
        this.badge = badge;
    }

    public String getUserId() { return userId; }
    public int getPosition() { return position; }
    public UserBadge getBadge() { return badge; }
}
