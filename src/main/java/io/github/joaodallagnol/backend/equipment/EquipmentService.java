package io.github.joaodallagnol.backend.equipment;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.session.EquipmentReference;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final EquipmentReferenceRepository equipmentRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;

    public EquipmentService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            EquipmentReferenceRepository equipmentRepository,
            HobbyRepository hobbyRepository,
            UserHobbyRepository userHobbyRepository
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.equipmentRepository = equipmentRepository;
        this.hobbyRepository = hobbyRepository;
        this.userHobbyRepository = userHobbyRepository;
    }

    @Transactional
    public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
        String userId = getAuthenticatedUser().id();
        Hobby hobby = resolveOptionalAllowedHobby(userId, request.hobbyId());
        EquipmentReference equipment = new EquipmentReference(
                userId,
                hobby,
                request.category().trim(),
                request.name().trim()
        );
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    public List<EquipmentResponse> listEquipment(UUID hobbyId) {
        String userId = getAuthenticatedUser().id();
        if (hobbyId != null) {
            resolveOptionalAllowedHobby(userId, hobbyId);
            return equipmentRepository.findAllByUserIdAndHobbyIdOrderByCategoryAscNameAsc(userId, hobbyId).stream()
                    .map(EquipmentResponse::from)
                    .toList();
        }
        return equipmentRepository.findAllByUserIdOrderByCategoryAscNameAsc(userId).stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    @Transactional
    public EquipmentResponse updateEquipment(UUID equipmentId, UpdateEquipmentRequest request) {
        String userId = getAuthenticatedUser().id();
        EquipmentReference equipment = getOwnedEquipment(equipmentId, userId);
        Hobby hobby = resolveOptionalAllowedHobby(userId, request.hobbyId());
        equipment.update(hobby, request.category().trim(), request.name().trim());
        return EquipmentResponse.from(equipment);
    }

    @Transactional
    public void deleteEquipment(UUID equipmentId) {
        String userId = getAuthenticatedUser().id();
        EquipmentReference equipment = getOwnedEquipment(equipmentId, userId);
        if (equipmentRepository.existsSessionUsageByUserIdAndEquipmentId(userId, equipmentId)) {
            throw new IllegalArgumentException("Equipment cannot be deleted because it is already linked to one or more sessions.");
        }
        equipmentRepository.delete(equipment);
    }

    private EquipmentReference getOwnedEquipment(UUID equipmentId, String userId) {
        return equipmentRepository.findByIdAndUserId(equipmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found."));
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
}
