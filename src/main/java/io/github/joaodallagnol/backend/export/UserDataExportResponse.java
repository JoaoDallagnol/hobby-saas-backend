package io.github.joaodallagnol.backend.export;

import io.github.joaodallagnol.backend.backlog.BacklogItemResponse;
import io.github.joaodallagnol.backend.equipment.EquipmentResponse;
import io.github.joaodallagnol.backend.user.CurrentUserProfileResponse;
import io.github.joaodallagnol.backend.user.UserHobbyResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record UserDataExportResponse(
        OffsetDateTime exportedAt,
        CurrentUserProfileResponse profile,
        List<UserHobbyResponse> hobbies,
        List<ExportSessionResponse> sessions,
        List<EquipmentResponse> equipment,
        List<BacklogItemResponse> backlogItems
) {
}
