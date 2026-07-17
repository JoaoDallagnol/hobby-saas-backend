package io.github.joaodallagnol.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUserRepository extends JpaRepository<ProductUser, String> {
}
