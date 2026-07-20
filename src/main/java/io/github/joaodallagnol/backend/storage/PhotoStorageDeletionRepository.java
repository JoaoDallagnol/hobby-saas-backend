package io.github.joaodallagnol.backend.storage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoStorageDeletionRepository extends JpaRepository<PhotoStorageDeletion, UUID> {

    List<PhotoStorageDeletion> findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(OffsetDateTime now);
}
