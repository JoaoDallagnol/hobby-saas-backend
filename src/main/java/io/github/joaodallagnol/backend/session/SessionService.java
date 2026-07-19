package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final SessionRecordRepository sessionRecordRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final EquipmentReferenceRepository equipmentReferenceRepository;
    private final BacklogItemReferenceRepository backlogItemReferenceRepository;
    private final PlaceReferenceRepository placeReferenceRepository;
    private final HobbyAttributeTemplateService hobbyAttributeTemplateService;
    private final PlaceResolutionService placeResolutionService;

    public SessionService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            SessionRecordRepository sessionRecordRepository,
            HobbyRepository hobbyRepository,
            UserHobbyRepository userHobbyRepository,
            EquipmentReferenceRepository equipmentReferenceRepository,
            BacklogItemReferenceRepository backlogItemReferenceRepository,
            PlaceReferenceRepository placeReferenceRepository,
            HobbyAttributeTemplateService hobbyAttributeTemplateService,
            PlaceResolutionService placeResolutionService
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.sessionRecordRepository = sessionRecordRepository;
        this.hobbyRepository = hobbyRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.equipmentReferenceRepository = equipmentReferenceRepository;
        this.backlogItemReferenceRepository = backlogItemReferenceRepository;
        this.placeReferenceRepository = placeReferenceRepository;
        this.hobbyAttributeTemplateService = hobbyAttributeTemplateService;
        this.placeResolutionService = placeResolutionService;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        Hobby hobby = resolveAllowedHobby(user.id(), request.hobbyId());
        hobbyAttributeTemplateService.validateAttributes(user.id(), hobby.getId(), request.attributes());
        validateProjectOwnership(request.projectId(), user.id());
        PlaceReference place = resolvePlace(request.location());
        Set<EquipmentReference> equipment = resolveEquipment(request.equipmentIds(), user.id());

        SessionRecord session = new SessionRecord(
                user.id(),
                hobby,
                request.title().trim(),
                request.startedAt(),
                request.durationMinutes(),
                request.notes(),
                request.satisfaction(),
                place == null ? null : place.getPlaceId(),
                request.projectId(),
                request.attributes()
        );
        session.assignPlace(place);
        session.replaceEquipment(equipment);
        session.replacePhotos(extractPhotoKeys(request.photos()));

        return SessionResponse.from(sessionRecordRepository.save(session));
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        SessionRecord session = getOwnedSession(sessionId, user.id());
        Hobby hobby = resolveAllowedHobby(user.id(), request.hobbyId());
        hobbyAttributeTemplateService.validateAttributes(user.id(), hobby.getId(), request.attributes());
        validateProjectOwnership(request.projectId(), user.id());
        PlaceReference place = resolvePlace(request.location());
        Set<EquipmentReference> equipment = resolveEquipment(request.equipmentIds(), user.id());

        session.update(
                hobby,
                request.title().trim(),
                request.startedAt(),
                request.durationMinutes(),
                request.notes(),
                request.satisfaction(),
                place == null ? null : place.getPlaceId(),
                request.projectId(),
                request.attributes()
        );
        session.assignPlace(place);
        session.replaceEquipment(equipment);
        session.replacePhotos(extractPhotoKeys(request.photos()));

        return SessionResponse.from(session);
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        sessionRecordRepository.delete(getOwnedSession(sessionId, getAuthenticatedUser().id()));
    }

    public List<SessionResponse> listSessions(UUID hobbyId) {
        String userId = getAuthenticatedUser().id();
        List<SessionRecord> sessions = hobbyId == null
                ? sessionRecordRepository.findAllByUserIdOrderByStartedAtDesc(userId)
                : sessionRecordRepository.findAllByUserIdAndHobbyIdOrderByStartedAtDesc(userId, hobbyId);
        return sessions.stream().map(SessionResponse::from).toList();
    }

    public SessionResponse getSession(UUID sessionId) {
        return SessionResponse.from(getOwnedSession(sessionId, getAuthenticatedUser().id()));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication());
    }

    private SessionRecord getOwnedSession(UUID sessionId, String userId) {
        return sessionRecordRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found."));
    }

    private Hobby resolveAllowedHobby(String userId, UUID hobbyId) {
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby not found."));
        if (!userHobbyRepository.existsByIdUserIdAndIdHobbyId(userId, hobbyId)) {
            throw new IllegalArgumentException("Hobby is not linked to the user profile.");
        }
        return hobby;
    }

    private void validateProjectOwnership(UUID projectId, String userId) {
        if (projectId != null && !backlogItemReferenceRepository.existsByIdAndUserId(projectId, userId)) {
            throw new IllegalArgumentException("Project not found for the user.");
        }
    }

    private PlaceReference resolvePlace(SessionLocationRequest location) {
        if (location == null) {
            return null;
        }
        return placeResolutionService.resolveOrCreate(location.placeId());
    }

    private Set<EquipmentReference> resolveEquipment(List<UUID> requestedIds, String userId) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> distinctIds = new LinkedHashSet<>(requestedIds);
        List<EquipmentReference> equipment = equipmentReferenceRepository.findAllByIdInAndUserId(distinctIds, userId);
        if (equipment.size() != distinctIds.size()) {
            throw new IllegalArgumentException("One or more equipment ids are invalid for the user.");
        }
        return new LinkedHashSet<>(equipment);
    }

    private List<String> extractPhotoKeys(List<SessionPhotoRequest> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }
        return photos.stream()
                .map(SessionPhotoRequest::storageKey)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .toList();
    }
}
