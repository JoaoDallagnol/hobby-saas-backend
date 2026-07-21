package io.github.joaodallagnol.backend.equipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentMaintenanceRuleRepository extends JpaRepository<EquipmentMaintenanceRule, UUID> {
    @EntityGraph(attributePaths = {"equipment"})
    List<EquipmentMaintenanceRule> findAllByEquipmentUserIdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = {"equipment"})
    Optional<EquipmentMaintenanceRule> findByIdAndEquipmentUserId(UUID id, String userId);
}
