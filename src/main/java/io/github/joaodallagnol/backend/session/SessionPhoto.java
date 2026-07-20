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

    @Column(name = "storage_key_thumbnail", length = 500)
    private String storageKeyThumbnail;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "processing_attempts", nullable = false)
    private int processingAttempts;

    @Column(name = "last_processing_error", length = 100)
    private String lastProcessingError;

    protected SessionPhoto() {
    }

    public SessionPhoto(SessionRecord session, String storageKeyOriginal) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.storageKeyOriginal = storageKeyOriginal;
        this.storageKeyThumbnail = null;
        this.processingStatus = "pending";
        this.processingAttempts = 0;
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

    public String getProcessingStatus() {
        return processingStatus;
    }

    public int getProcessingAttempts() {
        return processingAttempts;
    }

    public void markReady(String processedOriginalKey, String thumbnailKey) {
        this.storageKeyOriginal = processedOriginalKey;
        this.storageKeyThumbnail = thumbnailKey;
        this.processingStatus = "ready";
        this.lastProcessingError = null;
    }

    public void registerProcessingFailure(String failureCode) {
        this.processingAttempts++;
        this.lastProcessingError = failureCode;
        if (processingAttempts >= 3) {
            this.processingStatus = "failed";
        }
    }
}
