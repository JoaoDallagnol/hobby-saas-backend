package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.user.Hobby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "equipment")
public class EquipmentReference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 150)
    private String name;

    protected EquipmentReference() {
    }

    public EquipmentReference(String userId, Hobby hobby, String category, String name) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.hobby = hobby;
        this.category = category;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Hobby getHobby() {
        return hobby;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public void update(Hobby hobby, String category, String name) {
        this.hobby = hobby;
        this.category = category;
        this.name = name;
    }
}
