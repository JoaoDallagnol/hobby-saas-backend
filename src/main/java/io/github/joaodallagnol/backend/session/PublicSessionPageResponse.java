package io.github.joaodallagnol.backend.session;

import java.util.List;
import org.springframework.data.domain.Page;

public record PublicSessionPageResponse(
        List<PublicSessionResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {
    public static PublicSessionPageResponse from(Page<SessionRecord> result, SessionPhotoMediaService mediaService) {
        return new PublicSessionPageResponse(
                result.getContent().stream().map(session -> PublicSessionResponse.from(session, mediaService)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext()
        );
    }
}
