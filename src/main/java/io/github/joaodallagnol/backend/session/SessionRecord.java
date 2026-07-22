package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.user.Hobby;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sessions")
public class SessionRecord {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Column(nullable = false)
    private String title;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private int satisfaction;

    @Column(nullable = false, length = 20)
    private SessionVisibility visibility;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "location_label", length = 150)
    private String locationLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id", insertable = false, updatable = false)
    private PlaceReference place;

    @Column(name = "project_id")
    private UUID projectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionPhoto> photos = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "session_equipment",
            joinColumns = @JoinColumn(name = "session_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private Set<EquipmentReference> equipment = new LinkedHashSet<>();

    protected SessionRecord() {
    }

    public SessionRecord(
            String userId,
            Hobby hobby,
            String title,
            OffsetDateTime startedAt,
            int durationMinutes,
            String notes,
            int satisfaction,
            String placeId,
            String locationLabel,
            UUID projectId,
            Map<String, Object> attributes,
            SessionVisibility visibility
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.hobby = hobby;
        this.title = title;
        this.startedAt = startedAt;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
        this.satisfaction = satisfaction;
        validateLocation(placeId, locationLabel);
        this.placeId = placeId;
        this.locationLabel = locationLabel;
        this.projectId = projectId;
        this.attributes = attributes;
        this.visibility = visibility == null ? SessionVisibility.ONLY_ME : visibility;
    }

    public SessionRecord(String userId, Hobby hobby, String title, OffsetDateTime startedAt, int durationMinutes,
                         String notes, int satisfaction, String placeId, UUID projectId,
                         Map<String, Object> attributes, SessionVisibility visibility) {
        this(userId, hobby, title, startedAt, durationMinutes, notes, satisfaction, placeId, null, projectId,
                attributes, visibility);
    }

    public SessionRecord(String userId, Hobby hobby, String title, OffsetDateTime startedAt, int durationMinutes,
                         String notes, int satisfaction, String placeId, UUID projectId, Map<String, Object> attributes) {
        this(userId, hobby, title, startedAt, durationMinutes, notes, satisfaction, placeId, projectId, attributes,
                SessionVisibility.ONLY_ME);
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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public SessionVisibility getVisibility() {
        return visibility;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public PlaceReference getPlace() {
        return place;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public List<SessionPhoto> getPhotos() {
        return photos;
    }

    public Set<EquipmentReference> getEquipment() {
        return equipment;
    }

    public void update(
            Hobby hobby,
            String title,
            OffsetDateTime startedAt,
            int durationMinutes,
            String notes,
            int satisfaction,
            String placeId,
            String locationLabel,
            UUID projectId,
            Map<String, Object> attributes,
            SessionVisibility visibility
    ) {
        this.hobby = hobby;
        this.title = title;
        this.startedAt = startedAt;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
        this.satisfaction = satisfaction;
        validateLocation(placeId, locationLabel);
        this.placeId = placeId;
        this.locationLabel = locationLabel;
        this.projectId = projectId;
        this.attributes = attributes;
        this.visibility = visibility == null ? SessionVisibility.ONLY_ME : visibility;
    }

    public void update(Hobby hobby, String title, OffsetDateTime startedAt, int durationMinutes, String notes,
                       int satisfaction, String placeId, UUID projectId, Map<String, Object> attributes,
                       SessionVisibility visibility) {
        update(hobby, title, startedAt, durationMinutes, notes, satisfaction, placeId, null, projectId, attributes,
                visibility);
    }

    public void update(Hobby hobby, String title, OffsetDateTime startedAt, int durationMinutes, String notes,
                       int satisfaction, String placeId, UUID projectId, Map<String, Object> attributes) {
        update(hobby, title, startedAt, durationMinutes, notes, satisfaction, placeId, projectId, attributes,
                this.visibility == null ? SessionVisibility.ONLY_ME : this.visibility);
    }

    public void assignPlace(PlaceReference place, String locationLabel) {
        validateLocation(place == null ? null : place.getPlaceId(), locationLabel);
        this.place = place;
        this.placeId = place == null ? null : place.getPlaceId();
        this.locationLabel = place == null ? null : locationLabel;
    }

    private void validateLocation(String placeId, String locationLabel) {
        boolean hasPlace = placeId != null && !placeId.isBlank();
        boolean hasLabel = locationLabel != null && !locationLabel.isBlank();
        if (hasPlace != hasLabel) {
            throw new IllegalArgumentException("Place id and location label must be provided together.");
        }
    }

    public void replaceEquipment(Set<EquipmentReference> equipment) {
        this.equipment.clear();
        this.equipment.addAll(equipment);
    }

    public void replacePhotos(List<String> storageKeys) {
        requireAtMostOnePhoto(storageKeys.size());
        this.photos.clear();
        for (String storageKey : storageKeys) {
            this.photos.add(new SessionPhoto(this, storageKey));
        }
    }

    public void reconcilePhotos(Set<UUID> retainedPhotoIds, List<String> newStorageKeys) {
        requireAtMostOnePhoto(retainedPhotoIds.size() + newStorageKeys.size());
        this.photos.removeIf(photo -> !retainedPhotoIds.contains(photo.getId()));
        for (String storageKey : newStorageKeys) {
            this.photos.add(new SessionPhoto(this, storageKey));
        }
    }

    private void requireAtMostOnePhoto(int count) {
        if (count > 1) {
            throw new IllegalArgumentException("A session can contain at most one photo.");
        }
    }
}
