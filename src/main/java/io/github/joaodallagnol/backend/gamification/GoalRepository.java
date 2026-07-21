package io.github.joaodallagnol.backend.gamification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    @EntityGraph(attributePaths = {"hobby"})
    List<Goal> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = {"hobby"})
    Optional<Goal> findByIdAndUserId(UUID id, String userId);

    @Query("""
            select count(goal) from Goal goal
            where goal.userId = :userId
              and goal.hobby.id = :hobbyId
              and goal.status = :status
              and goal.cadence = :cadence
              and goal.startDate <= :periodEnd
              and goal.endDate >= :periodStart
            """)
    long countOverlapping(@Param("userId") String userId, @Param("hobbyId") UUID hobbyId,
                          @Param("status") GoalStatus status, @Param("cadence") GoalCadence cadence,
                          @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:scope))", nativeQuery = true)
    void lockScope(@Param("scope") String scope);
}
