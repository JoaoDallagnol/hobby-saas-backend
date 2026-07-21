package io.github.joaodallagnol.backend.session;

import java.util.List;
import org.springframework.data.domain.Page;

public record SessionPageResponse(
        List<SessionResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {

    static SessionPageResponse from(Page<SessionRecord> result, SessionPhotoMediaService mediaService) {
        return new SessionPageResponse(
                result.getContent().stream().map(session -> SessionResponse.from(session, mediaService)).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }
}
