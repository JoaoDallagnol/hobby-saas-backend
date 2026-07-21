package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import io.github.joaodallagnol.backend.feature.FeatureFlagService;
import io.github.joaodallagnol.backend.storage.SessionPhotoStorageKeyPolicy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

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
    private final FeatureFlagService featureFlagService;
    private final SessionPhotoMediaService sessionPhotoMediaService;

    @Autowired
    public SessionService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            SessionRecordRepository sessionRecordRepository,
            HobbyRepository hobbyRepository,
            UserHobbyRepository userHobbyRepository,
            EquipmentReferenceRepository equipmentReferenceRepository,
            BacklogItemReferenceRepository backlogItemReferenceRepository,
            PlaceReferenceRepository placeReferenceRepository,
            HobbyAttributeTemplateService hobbyAttributeTemplateService,
            PlaceResolutionService placeResolutionService,
            FeatureFlagService featureFlagService,
            SessionPhotoMediaService sessionPhotoMediaService
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
        this.featureFlagService = featureFlagService;
        this.sessionPhotoMediaService = sessionPhotoMediaService;
    }

    public SessionService(AuthenticatedUserExtractor authenticatedUserExtractor,
                          SessionRecordRepository sessionRecordRepository, HobbyRepository hobbyRepository,
                          UserHobbyRepository userHobbyRepository,
                          EquipmentReferenceRepository equipmentReferenceRepository,
                          BacklogItemReferenceRepository backlogItemReferenceRepository,
                          PlaceReferenceRepository placeReferenceRepository,
                          HobbyAttributeTemplateService hobbyAttributeTemplateService,
                          PlaceResolutionService placeResolutionService, FeatureFlagService featureFlagService) {
        this(authenticatedUserExtractor, sessionRecordRepository, hobbyRepository, userHobbyRepository,
                equipmentReferenceRepository, backlogItemReferenceRepository, placeReferenceRepository,
                hobbyAttributeTemplateService, placeResolutionService, featureFlagService,
                new SessionPhotoMediaService(storageKey -> null, ""));
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        Hobby hobby = resolveAllowedHobby(user.id(), request.hobbyId());
        hobbyAttributeTemplateService.validateAttributes(user.id(), hobby.getId(), request.attributes());
        validateProjectOwnership(request.projectId(), user.id(), hobby.getId());
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
                request.attributes(),
                request.visibility()
        );
        session.assignPlace(place);
        session.replaceEquipment(equipment);
        session.replacePhotos(extractNewPhotoKeys(request.photos(), user.id()));

        return SessionResponse.from(sessionRecordRepository.save(session), sessionPhotoMediaService);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, UpdateSessionRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        SessionRecord session = getOwnedSession(sessionId, user.id());
        Hobby hobby = resolveAllowedHobby(user.id(), request.hobbyId());
        hobbyAttributeTemplateService.validateAttributes(user.id(), hobby.getId(), request.attributes());
        validateProjectOwnership(request.projectId(), user.id(), hobby.getId());
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
                request.attributes(),
                request.visibility()
        );
        session.assignPlace(place);
        session.replaceEquipment(equipment);
        reconcilePhotos(session, request.photos(), user.id());

        return SessionResponse.from(session, sessionPhotoMediaService);
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        sessionRecordRepository.delete(getOwnedSession(sessionId, getAuthenticatedUser().id()));
    }

    @Transactional(readOnly = true)
    public SessionPageResponse listSessions(UUID hobbyId, int page, int size) {
        String userId = getAuthenticatedUser().id();
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"))
        );
        Page<SessionRecord> sessions = hobbyId == null
                ? sessionRecordRepository.findAllByUserId(userId, pageable)
                : sessionRecordRepository.findAllByUserIdAndHobbyId(userId, hobbyId, pageable);
        return SessionPageResponse.from(sessions, sessionPhotoMediaService);
    }

    public SessionResponse getSession(UUID sessionId) {
        return SessionResponse.from(getOwnedSession(sessionId, getAuthenticatedUser().id()), sessionPhotoMediaService);
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

    private void validateProjectOwnership(UUID projectId, String userId, UUID sessionHobbyId) {
        if (projectId == null) {
            return;
        }
        BacklogItemReference project = backlogItemReferenceRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found for the user."));
        if (project.getHobby() != null && !project.getHobby().getId().equals(sessionHobbyId)) {
            throw new IllegalArgumentException("Project belongs to a different hobby.");
        }
    }

    private PlaceReference resolvePlace(SessionLocationRequest location) {
        if (location == null) {
            return null;
        }
        featureFlagService.requireSessionLocation();
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

    private List<String> extractNewPhotoKeys(List<SessionPhotoRequest> photos, String userId) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }
        if (photos.stream().anyMatch(photo -> photo.id() != null)) {
            throw new IllegalArgumentException("Photo id cannot be used when creating a session.");
        }
        return validateNewPhotoKeys(photos, userId);
    }

    private void reconcilePhotos(SessionRecord session, List<SessionPhotoRequest> photos, String userId) {
        if (photos == null) {
            return;
        }

        Set<UUID> retainedIds = new LinkedHashSet<>();
        List<SessionPhotoRequest> newPhotos = photos.stream().filter(photo -> photo.id() == null).toList();
        for (SessionPhotoRequest photo : photos) {
            boolean hasId = photo.id() != null;
            boolean hasStorageKey = photo.storageKey() != null && !photo.storageKey().isBlank();
            if (hasId == hasStorageKey) {
                throw new IllegalArgumentException("Each photo must contain either an existing id or a new storage key.");
            }
            if (hasId && !retainedIds.add(photo.id())) {
                throw new IllegalArgumentException("Duplicate photo id.");
            }
        }

        Map<UUID, SessionPhoto> ownedPhotos = session.getPhotos().stream()
                .collect(java.util.stream.Collectors.toMap(SessionPhoto::getId, photo -> photo));
        if (!ownedPhotos.keySet().containsAll(retainedIds)) {
            throw new IllegalArgumentException("One or more photo ids do not belong to the session.");
        }
        session.reconcilePhotos(retainedIds, validateNewPhotoKeys(newPhotos, userId));
    }

    private List<String> validateNewPhotoKeys(List<SessionPhotoRequest> photos, String userId) {
        if (photos.isEmpty()) {
            return List.of();
        }
        if (photos.stream().anyMatch(photo -> photo.storageKey() == null || photo.storageKey().isBlank())) {
            throw new IllegalArgumentException("New photo storage key is required.");
        }
        featureFlagService.requirePhotoUploads();
        List<String> keys = photos.stream()
                .map(SessionPhotoRequest::storageKey)
                .map(String::trim)
                .toList();
        if (new LinkedHashSet<>(keys).size() != keys.size()) {
            throw new IllegalArgumentException("Duplicate photo storage key.");
        }
        keys.forEach(key -> SessionPhotoStorageKeyPolicy.requireOwnedUploadKey(userId, key));
        return keys;
    }
}
