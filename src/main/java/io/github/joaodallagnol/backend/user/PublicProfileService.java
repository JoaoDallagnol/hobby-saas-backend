package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.session.PublicSessionPageResponse;
import io.github.joaodallagnol.backend.session.PublicSessionResponse;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.session.SessionPhotoMediaService;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.session.SessionVisibility;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicProfileService {

    private final ProductUserRepository productUserRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final SessionRecordRepository sessionRecordRepository;
    private final SessionPhotoMediaService mediaService;

    public PublicProfileService(ProductUserRepository productUserRepository, UserHobbyRepository userHobbyRepository,
                                SessionRecordRepository sessionRecordRepository, SessionPhotoMediaService mediaService) {
        this.productUserRepository = productUserRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.sessionRecordRepository = sessionRecordRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getProfile(String username) {
        ProductUser user = getUser(username);
        return new PublicProfileResponse(user.getUsername(), user.getName(), user.getBio(),
                userHobbyRepository.findAllByIdUserIdOrderByHobbyNameAsc(user.getId()).stream()
                        .map(UserHobbyResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PublicSessionPageResponse listSessions(String username, UUID hobbyId, int page, int size) {
        ProductUser user = getUser(username);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));
        Page<SessionRecord> sessions = hobbyId == null
                ? sessionRecordRepository.findAllByUserIdAndVisibility(user.getId(), SessionVisibility.EVERYONE, pageable)
                : sessionRecordRepository.findAllByUserIdAndHobbyIdAndVisibility(
                        user.getId(), hobbyId, SessionVisibility.EVERYONE, pageable);
        return PublicSessionPageResponse.from(sessions, mediaService);
    }

    @Transactional(readOnly = true)
    public PublicSessionResponse getSession(String username, UUID sessionId) {
        ProductUser user = getUser(username);
        SessionRecord session = sessionRecordRepository
                .findByIdAndUserIdAndVisibility(sessionId, user.getId(), SessionVisibility.EVERYONE)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found."));
        return PublicSessionResponse.from(session, mediaService);
    }

    private ProductUser getUser(String username) {
        return productUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
    }
}
