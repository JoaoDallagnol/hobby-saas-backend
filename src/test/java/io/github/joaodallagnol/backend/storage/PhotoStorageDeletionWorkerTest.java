package io.github.joaodallagnol.backend.storage;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
class PhotoStorageDeletionWorkerTest {

    @Test
    void shouldRemoveQueueEntryAfterObjectDeletion() {
        AtomicReference<PhotoStorageDeletion> deleted = new AtomicReference<>();
        PhotoStorageDeletionRepository repository = repository(deleted, new AtomicReference<>());
        AtomicReference<String> deletedKey = new AtomicReference<>();
        R2ObjectStorage storage = storage(deletedKey, false);
        PhotoStorageDeletion deletion = deletion("processed/photo.webp");

        new PhotoStorageDeletionWorker(repository, storage).deleteOne(deletion);

        assertThat(deletedKey).hasValue("processed/photo.webp");
        assertThat(deleted).hasValue(deletion);
    }

    @Test
    void shouldPersistSafeRetryStateWhenStorageIsUnavailable() {
        AtomicReference<PhotoStorageDeletion> saved = new AtomicReference<>();
        PhotoStorageDeletionRepository repository = repository(new AtomicReference<>(), saved);
        R2ObjectStorage storage = storage(new AtomicReference<>(), true);
        PhotoStorageDeletion deletion = deletion("processed/photo.webp");

        new PhotoStorageDeletionWorker(repository, storage).deleteOne(deletion);

        assertThat(deletion.getAttempts()).isEqualTo(1);
        assertThat(saved).hasValue(deletion);
    }

    private PhotoStorageDeletion deletion(String storageKey) {
        PhotoStorageDeletion deletion = new PhotoStorageDeletion();
        ReflectionTestUtils.setField(deletion, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(deletion, "storageKey", storageKey);
        ReflectionTestUtils.setField(deletion, "createdAt", OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(deletion, "nextAttemptAt", OffsetDateTime.now(ZoneOffset.UTC));
        return deletion;
    }

    private PhotoStorageDeletionRepository repository(
            AtomicReference<PhotoStorageDeletion> deleted,
            AtomicReference<PhotoStorageDeletion> saved
    ) {
        return (PhotoStorageDeletionRepository) Proxy.newProxyInstance(
                PhotoStorageDeletionRepository.class.getClassLoader(),
                new Class<?>[]{PhotoStorageDeletionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "delete" -> {
                        deleted.set((PhotoStorageDeletion) args[0]);
                        yield null;
                    }
                    case "save" -> {
                        saved.set((PhotoStorageDeletion) args[0]);
                        yield args[0];
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "PhotoStorageDeletionRepositoryTestDouble";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private R2ObjectStorage storage(AtomicReference<String> deletedKey, boolean fail) {
        return new R2ObjectStorage() {
            @Override
            public void download(String storageKey, Path destination) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void uploadWebp(String storageKey, Path source) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(String storageKey) {
                if (fail) {
                    throw new IllegalStateException("provider detail must not be persisted");
                }
                deletedKey.set(storageKey);
            }
        };
    }
}
