package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionPhotoRepository extends JpaRepository<SessionPhoto, UUID> {

    List<SessionPhoto> findTop10ByProcessingStatusAndProcessingAttemptsLessThanOrderByIdAsc(
            String processingStatus,
            int maximumAttempts
    );
}
