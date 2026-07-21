package io.github.joaodallagnol.backend.gamification;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeaturedBadgeRepository extends JpaRepository<UserFeaturedBadge, UserFeaturedBadgeId> {
    @EntityGraph(attributePaths = {"badge", "badge.hobby"})
    List<UserFeaturedBadge> findAllByUserIdOrderByPositionAsc(String userId);
    void deleteAllByUserId(String userId);
}
