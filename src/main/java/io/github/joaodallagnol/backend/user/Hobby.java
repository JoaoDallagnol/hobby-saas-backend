package io.github.joaodallagnol.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "hobbies")
public class Hobby {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private HobbyCategory category;

    @Column(nullable = false)
    private String name;

    @Column
    private String icon;

    protected Hobby() {
    }

    public UUID getId() {
        return id;
    }

    public HobbyCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
