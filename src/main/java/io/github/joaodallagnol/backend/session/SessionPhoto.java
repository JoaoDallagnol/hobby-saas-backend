package io.github.joaodallagnol.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import io.github.joaodallagnol.backend.storage.StorageScope;

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

    @Column(name = "storage_scope", nullable = false, length = 20)
    private StorageScope storageScope;

    protected SessionPhoto() {
    }

    public SessionPhoto(SessionRecord session, String storageKeyOriginal) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.storageKeyOriginal = storageKeyOriginal;
        this.storageKeyThumbnail = null;
        this.processingStatus = "pending";
        this.processingAttempts = 0;
        this.storageScope = StorageScope.PRIVATE;
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

    public SessionRecord getSession() {
        return session;
    }

    public StorageScope getStorageScope() {
        return storageScope;
    }

    public void markReady(String processedOriginalKey, String thumbnailKey, StorageScope storageScope) {
        this.storageKeyOriginal = processedOriginalKey;
        this.storageKeyThumbnail = thumbnailKey;
        this.processingStatus = "ready";
        this.storageScope = storageScope;
        this.lastProcessingError = null;
    }

    public void markReady(String processedOriginalKey, String thumbnailKey) {
        markReady(processedOriginalKey, thumbnailKey, StorageScope.PRIVATE);
    }

    public void moveTo(StorageScope storageScope) {
        this.storageScope = storageScope;
    }

    public void registerProcessingFailure(String failureCode) {
        this.processingAttempts++;
        this.lastProcessingError = failureCode;
        if (processingAttempts >= 3) {
            this.processingStatus = "failed";
        }
    }
}
