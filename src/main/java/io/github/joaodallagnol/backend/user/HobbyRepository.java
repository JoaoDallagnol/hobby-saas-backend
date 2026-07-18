package io.github.joaodallagnol.backend.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyRepository extends JpaRepository<Hobby, UUID> {
}
