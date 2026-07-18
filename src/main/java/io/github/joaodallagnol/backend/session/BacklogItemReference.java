package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "backlog_items")
public class BacklogItemReference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    protected BacklogItemReference() {
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }
}
