package io.github.joaodallagnol.backend.gamification;

import io.github.joaodallagnol.backend.user.Hobby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private GoalMetric metric;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(nullable = false, length = 20)
    private GoalCadence cadence;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(nullable = false)
    private boolean advanced;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Goal() {
    }

    public Goal(String userId, Hobby hobby, String name, GoalMetric metric, int targetValue, GoalCadence cadence,
                LocalDate startDate, LocalDate endDate, boolean advanced, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.hobby = hobby;
        this.name = name;
        this.metric = metric;
        this.targetValue = targetValue;
        this.cadence = cadence;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = GoalStatus.ACTIVE;
        this.advanced = advanced;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public Hobby getHobby() { return hobby; }
    public String getName() { return name; }
    public GoalMetric getMetric() { return metric; }
    public int getTargetValue() { return targetValue; }
    public GoalCadence getCadence() { return cadence; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public GoalStatus getStatus() { return status; }
    public boolean isAdvanced() { return advanced; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void update(Hobby hobby, String name, GoalMetric metric, int targetValue, GoalCadence cadence,
                       LocalDate startDate, LocalDate endDate, boolean advanced) {
        this.hobby = hobby;
        this.name = name;
        this.metric = metric;
        this.targetValue = targetValue;
        this.cadence = cadence;
        this.startDate = startDate;
        this.endDate = endDate;
        this.advanced = advanced;
    }

    public void archive() {
        this.status = GoalStatus.ARCHIVED;
    }
}
