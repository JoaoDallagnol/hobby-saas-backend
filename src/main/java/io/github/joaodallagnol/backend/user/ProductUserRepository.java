package io.github.joaodallagnol.backend.user;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductUserRepository extends JpaRepository<ProductUser, String> {

    Optional<ProductUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, String id);

    @Modifying
    @Query(value = """
            INSERT INTO users (id, email, name, email_verified, bio, created_at)
            VALUES (:id, :email, :name, :emailVerified, NULL, :createdAt)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfMissing(
            @Param("id") String id,
            @Param("email") String email,
            @Param("name") String name,
            @Param("emailVerified") boolean emailVerified,
            @Param("createdAt") OffsetDateTime createdAt
    );
}
