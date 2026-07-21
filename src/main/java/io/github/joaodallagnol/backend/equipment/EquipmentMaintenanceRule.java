package io.github.joaodallagnol.backend.equipment;

import io.github.joaodallagnol.backend.session.EquipmentReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipment_maintenance_rules")
public class EquipmentMaintenanceRule {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentReference equipment;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes;

    @Column(name = "last_maintained_at")
    private OffsetDateTime lastMaintainedAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected EquipmentMaintenanceRule() {
    }

    public EquipmentMaintenanceRule(EquipmentReference equipment, String name, int intervalMinutes,
                                    OffsetDateTime lastMaintainedAt, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.equipment = equipment;
        this.name = name;
        this.intervalMinutes = intervalMinutes;
        this.lastMaintainedAt = lastMaintainedAt;
        this.active = true;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public EquipmentReference getEquipment() { return equipment; }
    public String getName() { return name; }
    public int getIntervalMinutes() { return intervalMinutes; }
    public OffsetDateTime getLastMaintainedAt() { return lastMaintainedAt; }
    public boolean isActive() { return active; }

    public void update(String name, int intervalMinutes, OffsetDateTime lastMaintainedAt, boolean active) {
        this.name = name;
        this.intervalMinutes = intervalMinutes;
        this.lastMaintainedAt = lastMaintainedAt;
        this.active = active;
    }

    public void markMaintained(OffsetDateTime at) { this.lastMaintainedAt = at; }
}
