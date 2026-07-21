package io.github.joaodallagnol.backend.backlog;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.session.BacklogItemReference;
import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BacklogService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final BacklogItemReferenceRepository backlogRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final EntitlementService entitlementService;

    @Autowired
    public BacklogService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            BacklogItemReferenceRepository backlogRepository,
            HobbyRepository hobbyRepository,
            UserHobbyRepository userHobbyRepository,
            EntitlementService entitlementService
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.backlogRepository = backlogRepository;
        this.hobbyRepository = hobbyRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.entitlementService = entitlementService;
    }

    public BacklogService(AuthenticatedUserExtractor authenticatedUserExtractor,
                          BacklogItemReferenceRepository backlogRepository, HobbyRepository hobbyRepository,
                          UserHobbyRepository userHobbyRepository) {
        this(authenticatedUserExtractor, backlogRepository, hobbyRepository, userHobbyRepository, null);
    }

    @Transactional
    public BacklogItemResponse createItem(CreateBacklogItemRequest request) {
        String userId = getAuthenticatedUser().id();
        Hobby hobby = resolveOptionalAllowedHobby(userId, request.hobbyId());
        String status = BacklogStatus.from(request.status()).value();
        BacklogItemReference item = new BacklogItemReference(userId, hobby, request.title().trim(), status);
        applyPlanning(userId, item, request.dueDate(), request.priority(), request.archived(), request.position(), false);
        return BacklogItemResponse.from(backlogRepository.save(item));
    }

    public List<BacklogItemResponse> listItems(UUID hobbyId) {
        String userId = getAuthenticatedUser().id();
        if (hobbyId != null) {
            resolveOptionalAllowedHobby(userId, hobbyId);
            return backlogRepository.findAllByUserIdAndHobbyIdOrderByCreatedAtDesc(userId, hobbyId).stream()
                    .map(BacklogItemResponse::from)
                    .toList();
        }
        return backlogRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BacklogItemResponse::from)
                .toList();
    }

    @Transactional
    public BacklogItemResponse updateItem(UUID itemId, UpdateBacklogItemRequest request) {
        String userId = getAuthenticatedUser().id();
        BacklogItemReference item = getOwnedItem(itemId, userId);
        Hobby hobby = resolveOptionalAllowedHobby(userId, request.hobbyId());
        String status = BacklogStatus.from(request.status()).value();
        item.update(hobby, request.title().trim(), status);
        applyPlanning(userId, item, request.dueDate(), request.priority(), request.archived(), request.position(), true);
        return BacklogItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        String userId = getAuthenticatedUser().id();
        BacklogItemReference item = getOwnedItem(itemId, userId);
        if (backlogRepository.existsSessionUsageByUserIdAndProjectId(userId, itemId)) {
            throw new IllegalArgumentException("Backlog item cannot be deleted because it is already linked to one or more sessions.");
        }
        backlogRepository.delete(item);
    }

    private BacklogItemReference getOwnedItem(UUID itemId, String userId) {
        return backlogRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Backlog item not found."));
    }

    private Hobby resolveOptionalAllowedHobby(String userId, UUID hobbyId) {
        if (hobbyId == null) {
            return null;
        }
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new ResourceNotFoundException("Hobby not found."));
        if (!userHobbyRepository.existsByIdUserIdAndIdHobbyId(userId, hobbyId)) {
            throw new IllegalArgumentException("Hobby is not linked to the user profile.");
        }
        return hobby;
    }

    private AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication());
    }

    private void applyPlanning(String userId, BacklogItemReference item, java.time.LocalDate dueDate,
                               String requestedPriority, Boolean requestedArchived, Integer requestedPosition,
                               boolean preserveWhenOmitted) {
        if (preserveWhenOmitted && dueDate == null && requestedPriority == null
                && requestedArchived == null && requestedPosition == null) {
            return;
        }
        String priority = requestedPriority == null ? "normal" : requestedPriority.trim().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("low", "normal", "high").contains(priority)) {
            throw new IllegalArgumentException("priority must be 'low', 'normal' or 'high'.");
        }
        boolean archived = Boolean.TRUE.equals(requestedArchived);
        int position = requestedPosition == null ? 0 : requestedPosition;
        if (position < 0) {
            throw new IllegalArgumentException("position must be zero or greater.");
        }
        boolean advanced = dueDate != null || !"normal".equals(priority) || archived || position != 0;
        if (advanced) {
            if (entitlementService == null) {
                throw new IllegalArgumentException("Plus planning is unavailable.");
            }
            entitlementService.requirePlus(userId);
        }
        item.updatePlanning(dueDate, priority, archived, position);
    }
}
