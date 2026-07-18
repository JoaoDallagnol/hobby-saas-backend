package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyAttributeTemplateRepository extends JpaRepository<HobbyAttributeTemplate, UUID> {

    List<HobbyAttributeTemplate> findAllByHobbyIdOrderByDisplayOrderAsc(UUID hobbyId);
}
