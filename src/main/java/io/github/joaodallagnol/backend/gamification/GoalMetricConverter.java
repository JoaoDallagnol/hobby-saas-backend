package io.github.joaodallagnol.backend.gamification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GoalMetricConverter implements AttributeConverter<GoalMetric, String> {
    public String convertToDatabaseColumn(GoalMetric value) { return value == null ? null : value.value(); }
    public GoalMetric convertToEntityAttribute(String value) { return value == null ? null : GoalMetric.from(value); }
}
