package io.github.joaodallagnol.backend.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateSessionRequest(
        @NotNull
        UUID hobbyId,
        @NotBlank
        @Size(max = 255)
        String title,
        @NotNull
        OffsetDateTime startedAt,
        @Positive
        int durationMinutes,
        @Size(max = 5000)
        String notes,
        @Min(1)
        @Max(5)
        int satisfaction,
        @Valid
        SessionLocationRequest location,
        UUID projectId,
        List<UUID> equipmentIds,
        @Size(max = 10)
        List<@Valid SessionPhotoRequest> photos,
        Map<String, Object> attributes
) {
}
