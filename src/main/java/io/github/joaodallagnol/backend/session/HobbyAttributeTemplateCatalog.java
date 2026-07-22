package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.joaodallagnol.backend.config.CacheNames.HOBBY_ATTRIBUTE_TEMPLATES;

@Service
public class HobbyAttributeTemplateCatalog {

    private final HobbyAttributeTemplateRepository repository;

    public HobbyAttributeTemplateCatalog(HobbyAttributeTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = HOBBY_ATTRIBUTE_TEMPLATES, key = "#hobbyId", sync = true)
    public List<HobbyAttributeTemplate> findByHobbyId(UUID hobbyId) {
        return List.copyOf(repository.findAllByHobbyIdOrderByDisplayOrderAsc(hobbyId));
    }
}
