package io.github.joaodallagnol.backend.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUserRepository extends JpaRepository<ProductUser, UUID> {
}
