package io.github.joaodallagnol.backend.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHobbyRepository extends JpaRepository<UserHobby, UserHobbyId> {

    List<UserHobby> findAllByIdUserIdOrderByHobbyNameAsc(String userId);

    Optional<UserHobby> findByIdUserIdAndIdHobbyId(String userId, UUID hobbyId);

    boolean existsByIdUserIdAndIdHobbyId(String userId, UUID hobbyId);
}
