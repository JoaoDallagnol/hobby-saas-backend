package io.github.joaodallagnol.backend.gamification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GoalCadenceConverter implements AttributeConverter<GoalCadence, String> {
    public String convertToDatabaseColumn(GoalCadence value) { return value == null ? null : value.value(); }
    public GoalCadence convertToEntityAttribute(String value) { return value == null ? null : GoalCadence.from(value); }
}
