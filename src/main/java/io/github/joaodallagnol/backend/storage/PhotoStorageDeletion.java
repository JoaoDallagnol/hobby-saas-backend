package io.github.joaodallagnol.backend.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "photo_storage_deletions")
public class PhotoStorageDeletion {

    @Id
    private UUID id;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "storage_scope", nullable = false, length = 20)
    private StorageScope storageScope;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 100)
    private String lastError;

    protected PhotoStorageDeletion() {
    }

    public PhotoStorageDeletion(StorageScope storageScope, String storageKey) {
        this.id = UUID.randomUUID();
        this.storageScope = storageScope;
        this.storageKey = storageKey;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.attempts = 0;
        this.nextAttemptAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public StorageScope getStorageScope() {
        return storageScope == null ? StorageScope.PRIVATE : storageScope;
    }

    public int getAttempts() {
        return attempts;
    }

    public void registerFailure(String errorCode) {
        attempts++;
        long delayMinutes = Math.min(60, 1L << Math.min(attempts, 6));
        nextAttemptAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(delayMinutes);
        lastError = errorCode;
    }
}
