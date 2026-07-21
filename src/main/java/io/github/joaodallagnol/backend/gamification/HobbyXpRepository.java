package io.github.joaodallagnol.backend.gamification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HobbyXpRepository extends JpaRepository<HobbyXp, HobbyXpId> {
    List<HobbyXp> findAllByUserId(String userId);
    void deleteAllByUserId(String userId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:scope))", nativeQuery = true)
    void lockUserProjection(@Param("scope") String scope);
}
