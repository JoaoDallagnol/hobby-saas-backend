package io.github.joaodallagnol.backend.storage;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PhotoStorageDeletionWorker {

    private final PhotoStorageDeletionRepository repository;
    private final R2ObjectStorage objectStorage;

    public PhotoStorageDeletionWorker(
            PhotoStorageDeletionRepository repository,
            R2ObjectStorage objectStorage
    ) {
        this.repository = repository;
        this.objectStorage = objectStorage;
    }

    @Scheduled(fixedDelayString = "${app.photo-processing.deletion-poll-delay-ms:30000}")
    public void deleteDueObjects() {
        repository.findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OffsetDateTime.now(ZoneOffset.UTC))
                .forEach(this::deleteOne);
    }

    void deleteOne(PhotoStorageDeletion deletion) {
        try {
            objectStorage.delete(deletion.getStorageKey());
            repository.delete(deletion);
        } catch (RuntimeException ex) {
            deletion.registerFailure(ex.getClass().getSimpleName());
            repository.save(deletion);
        }
    }
}
