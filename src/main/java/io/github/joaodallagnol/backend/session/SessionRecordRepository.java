package io.github.joaodallagnol.backend.session;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    Page<SessionRecord> findAllByUserId(String userId, Pageable pageable);

    Page<SessionRecord> findAllByUserIdAndHobbyId(String userId, UUID hobbyId, Pageable pageable);

    @Query("select session.startedAt from SessionRecord session where session.userId = :userId order by session.startedAt desc")
    List<OffsetDateTime> findStartedAtByUserIdOrderByStartedAtDesc(String userId);

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "equipment", "place"})
    Optional<SessionRecord> findByIdAndUserId(UUID id, String userId);
}
