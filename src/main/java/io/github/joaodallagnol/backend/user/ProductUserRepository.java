package io.github.joaodallagnol.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductUserRepository extends JpaRepository<ProductUser, String> {
    Optional<ProductUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, String id);
}
