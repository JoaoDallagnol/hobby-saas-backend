package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "places")
public class PlaceReference {

    @Id
    @Column(name = "place_id", nullable = false, length = 255)
    private String placeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    protected PlaceReference() {
    }

    public PlaceReference(String placeId, String name, BigDecimal lat, BigDecimal lng) {
        this.placeId = placeId;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLng() {
        return lng;
    }
}
