package io.github.joaodallagnol.backend.session;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacklogItemReferenceRepository extends JpaRepository<BacklogItemReference, UUID> {

    boolean existsByIdAndUserId(UUID id, String userId);
}
