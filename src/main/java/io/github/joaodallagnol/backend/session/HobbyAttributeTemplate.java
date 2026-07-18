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
@Table(name = "hobby_attribute_template")
public class HobbyAttributeTemplate {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Column(nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(length = 50)
    private String unit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected HobbyAttributeTemplate() {
    }

    public UUID getId() {
        return id;
    }

    public Hobby getHobby() {
        return hobby;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
