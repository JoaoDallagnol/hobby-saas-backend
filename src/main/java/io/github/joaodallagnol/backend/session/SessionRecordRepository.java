package io.github.joaodallagnol.backend.session;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.github.joaodallagnol.backend.gamification.SessionMetricRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    Page<SessionRecord> findAllByUserId(String userId, Pageable pageable);

    @EntityGraph(attributePaths = {"hobby", "photos", "equipment", "place"})
    List<SessionRecord> findAllByUserIdOrderByStartedAtDesc(String userId);

    Page<SessionRecord> findAllByUserIdAndHobbyId(String userId, UUID hobbyId, Pageable pageable);

    Page<SessionRecord> findAllByUserIdAndVisibility(String userId, SessionVisibility visibility, Pageable pageable);

    Page<SessionRecord> findAllByUserIdAndHobbyIdAndVisibility(
            String userId, UUID hobbyId, SessionVisibility visibility, Pageable pageable);

    @Query("select session.startedAt from SessionRecord session where session.userId = :userId order by session.startedAt desc")
    List<OffsetDateTime> findStartedAtByUserIdOrderByStartedAtDesc(String userId);

    @Query("""
            select new io.github.joaodallagnol.backend.gamification.SessionMetricRow(
                session.hobby.id,
                session.hobby.name,
                session.hobby.category.name,
                session.hobby.category.xpSessionBonus,
                session.hobby.category.xpMinutesPerPoint,
                session.durationMinutes,
                session.startedAt
            )
            from SessionRecord session
            where session.userId = :userId
            order by session.startedAt desc
            """)
    List<SessionMetricRow> findGamificationMetricsByUserId(@Param("userId") String userId);

    @Query("""
            select new io.github.joaodallagnol.backend.gamification.SessionMetricRow(
                session.hobby.id,
                session.hobby.name,
                session.hobby.category.name,
                session.hobby.category.xpSessionBonus,
                session.hobby.category.xpMinutesPerPoint,
                session.durationMinutes,
                session.startedAt
            )
            from SessionRecord session
            where session.userId = :userId
              and session.startedAt >= :start
              and session.startedAt < :end
            order by session.startedAt desc
            """)
    List<SessionMetricRow> findGamificationMetricsByUserIdAndPeriod(
            @Param("userId") String userId, @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    long countByUserIdAndHobbyIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            String userId, UUID hobbyId, OffsetDateTime start, OffsetDateTime end);

    long countByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            String userId, OffsetDateTime start, OffsetDateTime end);

    @Query("""
            select coalesce(sum(session.durationMinutes), 0)
            from SessionRecord session
            where session.userId = :userId
              and (:hobbyId is null or session.hobby.id = :hobbyId)
              and session.startedAt >= :start
              and session.startedAt < :end
            """)
    long sumDurationMinutes(@Param("userId") String userId, @Param("hobbyId") UUID hobbyId,
                            @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("""
            select coalesce(sum(session.durationMinutes), 0)
            from SessionRecord session
            join session.equipment equipment
            where session.userId = :userId
              and equipment.id = :equipmentId
            """)
    long sumDurationMinutesForEquipment(@Param("userId") String userId,
                                        @Param("equipmentId") UUID equipmentId);

    @Query("""
            select coalesce(sum(session.durationMinutes), 0)
            from SessionRecord session
            join session.equipment equipment
            where session.userId = :userId
              and equipment.id = :equipmentId
              and session.startedAt >= :since
            """)
    long sumDurationMinutesForEquipmentSince(@Param("userId") String userId,
                                             @Param("equipmentId") UUID equipmentId,
                                             @Param("since") OffsetDateTime since);

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "equipment", "place"})
    Optional<SessionRecord> findByIdAndUserId(UUID id, String userId);

    @EntityGraph(attributePaths = {"hobby", "hobby.category", "photos", "place"})
    Optional<SessionRecord> findByIdAndUserIdAndVisibility(UUID id, String userId, SessionVisibility visibility);
}
