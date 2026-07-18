package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "equipment", "place"})
    List<SessionRecord> findAllByUserIdOrderByStartedAtDesc(String userId);

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "equipment", "place"})
    List<SessionRecord> findAllByUserIdAndHobbyIdOrderByStartedAtDesc(String userId, UUID hobbyId);

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "equipment", "place"})
    Optional<SessionRecord> findByIdAndUserId(UUID id, String userId);
}
