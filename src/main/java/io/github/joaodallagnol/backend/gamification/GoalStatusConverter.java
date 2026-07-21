package io.github.joaodallagnol.backend.gamification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

@Converter(autoApply = true)
public class GoalStatusConverter implements AttributeConverter<GoalStatus, String> {
    public String convertToDatabaseColumn(GoalStatus value) { return value == null ? null : value.value(); }
    public GoalStatus convertToEntityAttribute(String value) {
        return value == null ? null : Arrays.stream(GoalStatus.values())
                .filter(item -> item.value().equals(value)).findFirst().orElseThrow();
    }
}
