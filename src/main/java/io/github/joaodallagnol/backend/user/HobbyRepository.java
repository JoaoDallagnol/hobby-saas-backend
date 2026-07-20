package io.github.joaodallagnol.backend.user;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyRepository extends JpaRepository<Hobby, UUID> {

    @EntityGraph(attributePaths = "category")
    List<Hobby> findAllByOrderByCategoryNameAscNameAsc();
}
