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

    protected HobbyCategory() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
