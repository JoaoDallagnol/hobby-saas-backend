package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface SessionPhotoRepository extends JpaRepository<SessionPhoto, UUID> {

    List<SessionPhoto> findTop10ByProcessingStatusAndProcessingAttemptsLessThanOrderByIdAsc(
            String processingStatus,
            int maximumAttempts
    );

    @EntityGraph(attributePaths = {"session"})
    @Query("""
            select photo from SessionPhoto photo
            where photo.processingStatus = 'ready'
              and ((photo.session.visibility = io.github.joaodallagnol.backend.session.SessionVisibility.EVERYONE
                    and photo.storageScope <> io.github.joaodallagnol.backend.storage.StorageScope.PUBLIC)
                or (photo.session.visibility = io.github.joaodallagnol.backend.session.SessionVisibility.ONLY_ME
                    and photo.storageScope <> io.github.joaodallagnol.backend.storage.StorageScope.PRIVATE))
            order by photo.id
            """)
    List<SessionPhoto> findPhotosAwaitingStorageScopeChange(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"session"})
    Optional<SessionPhoto> findById(UUID id);
}
