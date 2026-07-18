package io.github.joaodallagnol.backend.equipment;

import io.github.joaodallagnol.backend.session.EquipmentReference;
import java.util.UUID;

public record EquipmentResponse(
        UUID id,
        UUID hobbyId,
        String hobbyName,
        String category,
        String name
) {

    public static EquipmentResponse from(EquipmentReference equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getHobby() == null ? null : equipment.getHobby().getId(),
                equipment.getHobby() == null ? null : equipment.getHobby().getName(),
                equipment.getCategory(),
                equipment.getName()
        );
    }
}
