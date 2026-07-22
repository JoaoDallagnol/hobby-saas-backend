package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "places")
public class PlaceReference {

    @Id
    @Column(name = "place_id", nullable = false, length = 255)
    private String placeId;

    @Column(name = "validated_at", nullable = false)
    private OffsetDateTime validatedAt;

    protected PlaceReference() {
    }

    public PlaceReference(String placeId, OffsetDateTime validatedAt) {
        this.placeId = placeId;
        this.validatedAt = validatedAt;
    }

    public String getPlaceId() {
        return placeId;
    }

    public OffsetDateTime getValidatedAt() {
        return validatedAt;
    }

    public void markValidatedAt(OffsetDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
}
