package io.github.joaodallagnol.backend.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(nullable = false, length = 20)
    private String plan;

    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(length = 50)
    private String provider;

    @Column(name = "external_subscription_id")
    private String externalSubscriptionId;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Subscription() {
    }

    public String getUserId() {
        return userId;
    }

    public String getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }
}
