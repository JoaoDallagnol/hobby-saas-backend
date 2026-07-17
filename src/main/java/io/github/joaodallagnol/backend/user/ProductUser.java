package io.github.joaodallagnol.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class ProductUser {

    @Id
    @Column(nullable = false, length = 128)
    private String id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ProductUser() {
    }

    public ProductUser(String id, String email, String name, boolean emailVerified, String bio, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.emailVerified = emailVerified;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getBio() {
        return bio;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
