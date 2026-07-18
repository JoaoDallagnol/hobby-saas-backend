package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "session_photos")
public class SessionPhoto {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionRecord session;

    @Column(name = "storage_key_original", nullable = false, length = 500)
    private String storageKeyOriginal;

    @Column(name = "storage_key_thumbnail", nullable = false, length = 500)
    private String storageKeyThumbnail;

    protected SessionPhoto() {
    }

    public SessionPhoto(SessionRecord session, String storageKeyOriginal) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.storageKeyOriginal = storageKeyOriginal;
        this.storageKeyThumbnail = storageKeyOriginal;
    }

    public UUID getId() {
        return id;
    }

    public String getStorageKeyOriginal() {
        return storageKeyOriginal;
    }

    public String getStorageKeyThumbnail() {
        return storageKeyThumbnail;
    }
}
