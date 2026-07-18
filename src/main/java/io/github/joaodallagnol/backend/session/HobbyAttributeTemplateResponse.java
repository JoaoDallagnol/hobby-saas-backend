package io.github.joaodallagnol.backend.session;

import java.util.UUID;

public record HobbyAttributeTemplateResponse(
        UUID id,
        String key,
        String label,
        String type,
        String unit,
        int displayOrder
) {

    public static HobbyAttributeTemplateResponse from(HobbyAttributeTemplate template) {
        return new HobbyAttributeTemplateResponse(
                template.getId(),
                template.getKey(),
                template.getLabel(),
                template.getType(),
                template.getUnit(),
                template.getDisplayOrder()
        );
    }
}
