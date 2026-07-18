package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "places")
public class PlaceReference {

    @Id
    @Column(name = "place_id", nullable = false, length = 255)
    private String placeId;

    protected PlaceReference() {
    }

    public String getPlaceId() {
        return placeId;
    }
}
