package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EquipmentReferenceRepository extends JpaRepository<EquipmentReference, UUID> {

    List<EquipmentReference> findAllByIdInAndUserId(Set<UUID> ids, String userId);

    List<EquipmentReference> findAllByUserIdOrderByCategoryAscNameAsc(String userId);

    List<EquipmentReference> findAllByUserIdAndHobbyIdOrderByCategoryAscNameAsc(String userId, UUID hobbyId);

    Optional<EquipmentReference> findByIdAndUserId(UUID id, String userId);

    @Query("""
            select (count(s) > 0)
            from SessionRecord s
            join s.equipment e
            where s.userId = :userId
              and e.id = :equipmentId
            """)
    boolean existsSessionUsageByUserIdAndEquipmentId(String userId, UUID equipmentId);
}
