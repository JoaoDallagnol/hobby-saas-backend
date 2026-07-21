package io.github.joaodallagnol.backend.session;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SessionVisibilityConverter implements AttributeConverter<SessionVisibility, String> {

    @Override
    public String convertToDatabaseColumn(SessionVisibility attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public SessionVisibility convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SessionVisibility.fromValue(dbData);
    }
}
