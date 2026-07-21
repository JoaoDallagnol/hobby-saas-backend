package io.github.joaodallagnol.backend.gamification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    @EntityGraph(attributePaths = {"hobby"})
    List<UserBadge> findAllByUserIdOrderByEarnedAtDesc(String userId);
}
