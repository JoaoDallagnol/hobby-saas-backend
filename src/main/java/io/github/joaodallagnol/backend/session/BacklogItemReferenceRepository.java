package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BacklogItemReferenceRepository extends JpaRepository<BacklogItemReference, UUID> {

    boolean existsByIdAndUserId(UUID id, String userId);

    List<BacklogItemReference> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<BacklogItemReference> findAllByUserIdAndHobbyIdOrderByCreatedAtDesc(String userId, UUID hobbyId);

    Optional<BacklogItemReference> findByIdAndUserId(UUID id, String userId);

    @Query("""
            select (count(s) > 0)
            from SessionRecord s
            where s.userId = :userId
              and s.projectId = :projectId
            """)
    boolean existsSessionUsageByUserIdAndProjectId(String userId, UUID projectId);
}
