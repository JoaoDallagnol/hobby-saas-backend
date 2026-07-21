package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.user.Hobby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.LocalDate;

@Entity
@Table(name = "backlog_items")
public class BacklogItemReference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String priority = "normal";

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false)
    private int position;

    protected BacklogItemReference() {
    }

    public BacklogItemReference(String userId, Hobby hobby, String title, String status) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.hobby = hobby;
        this.title = title;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
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

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public boolean isArchived() { return archived; }
    public int getPosition() { return position; }

    public void update(Hobby hobby, String title, String status) {
        this.hobby = hobby;
        this.title = title;
        this.status = status;
    }

    public void updatePlanning(LocalDate dueDate, String priority, boolean archived, int position) {
        this.dueDate = dueDate;
        this.priority = priority;
        this.archived = archived;
        this.position = position;
    }
}
