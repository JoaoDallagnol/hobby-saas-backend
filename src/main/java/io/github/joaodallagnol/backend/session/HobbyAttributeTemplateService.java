package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class HobbyAttributeTemplateService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyAttributeTemplateCatalog templateCatalog;

    @Autowired
    public HobbyAttributeTemplateService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            HobbyRepository hobbyRepository,
            UserHobbyRepository userHobbyRepository,
            HobbyAttributeTemplateCatalog templateCatalog
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.hobbyRepository = hobbyRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.templateCatalog = templateCatalog;
    }

    public HobbyAttributeTemplateService(AuthenticatedUserExtractor authenticatedUserExtractor,
                                         HobbyRepository hobbyRepository,
                                         UserHobbyRepository userHobbyRepository,
                                         HobbyAttributeTemplateRepository templateRepository) {
        this(authenticatedUserExtractor, hobbyRepository, userHobbyRepository,
                new HobbyAttributeTemplateCatalog(templateRepository));
    }

    public List<HobbyAttributeTemplateResponse> listTemplates(UUID hobbyId) {
        String userId = authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication()).id();
        ensureAllowedHobby(userId, hobbyId);
        return templateCatalog.findByHobbyId(hobbyId).stream()
                .map(HobbyAttributeTemplateResponse::from)
                .toList();
    }

    public void validateAttributes(String userId, UUID hobbyId, Map<String, Object> attributes) {
        ensureAllowedHobby(userId, hobbyId);
        List<HobbyAttributeTemplate> templates = templateCatalog.findByHobbyId(hobbyId);
        Map<String, HobbyAttributeTemplate> templatesByKey = templates.stream()
                .collect(java.util.stream.Collectors.toMap(HobbyAttributeTemplate::getKey, template -> template));

        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            HobbyAttributeTemplate template = templatesByKey.get(entry.getKey());
            if (template == null) {
                throw new IllegalArgumentException("Attribute key is not allowed for the selected hobby: " + entry.getKey());
            }
            validateType(template, entry.getValue());
        }
    }

    private void ensureAllowedHobby(String userId, UUID hobbyId) {
        hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby not found."));
        if (!userHobbyRepository.existsByIdUserIdAndIdHobbyId(userId, hobbyId)) {
            throw new IllegalArgumentException("Hobby is not linked to the user profile.");
        }
    }

    private void validateType(HobbyAttributeTemplate template, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Attribute value cannot be null: " + template.getKey());
        }

        boolean valid = switch (template.getType()) {
            case "number" -> value instanceof Number;
            case "text", "select" -> value instanceof String && !((String) value).isBlank();
            case "boolean" -> value instanceof Boolean;
            default -> throw new IllegalArgumentException("Unsupported attribute template type: " + template.getType());
        };

        if (!valid) {
            throw new IllegalArgumentException("Attribute value has invalid type for key: " + template.getKey());
        }
    }
}
