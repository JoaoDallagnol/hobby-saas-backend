package io.github.joaodallagnol.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "hobby_categories")
public class HobbyCategory {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "xp_session_bonus", nullable = false)
    private int xpSessionBonus;

    @Column(name = "xp_minutes_per_point", nullable = false)
    private int xpMinutesPerPoint;

    protected HobbyCategory() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getXpSessionBonus() {
        return xpSessionBonus;
    }

    public int getXpMinutesPerPoint() {
        return xpMinutesPerPoint;
    }
}
