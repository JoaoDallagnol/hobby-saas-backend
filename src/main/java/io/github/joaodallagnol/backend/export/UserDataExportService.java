package io.github.joaodallagnol.backend.export;

import io.github.joaodallagnol.backend.backlog.BacklogItemResponse;
import io.github.joaodallagnol.backend.equipment.EquipmentResponse;
import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.CurrentUserProfileResponse;
import io.github.joaodallagnol.backend.user.ProductUserRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyResponse;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDataExportService {
    private final EntitlementService entitlementService;
    private final ProductUserRepository userRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final SessionRecordRepository sessionRepository;
    private final EquipmentReferenceRepository equipmentRepository;
    private final BacklogItemReferenceRepository backlogRepository;
    private final Clock clock;

    public UserDataExportService(EntitlementService entitlementService, ProductUserRepository userRepository,
                                 UserHobbyRepository userHobbyRepository, SessionRecordRepository sessionRepository,
                                 EquipmentReferenceRepository equipmentRepository,
                                 BacklogItemReferenceRepository backlogRepository, Clock clock) {
        this.entitlementService = entitlementService;
        this.userRepository = userRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.sessionRepository = sessionRepository;
        this.equipmentRepository = equipmentRepository;
        this.backlogRepository = backlogRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserDataExportResponse json() {
        String userId = entitlementService.currentUserId();
        var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserDataExportResponse(OffsetDateTime.now(clock), CurrentUserProfileResponse.from(user),
                userHobbyRepository.findAllByIdUserIdOrderByHobbyNameAsc(userId).stream()
                        .map(UserHobbyResponse::from).toList(),
                sessionRepository.findAllByUserIdOrderByStartedAtDesc(userId).stream()
                        .map(ExportSessionResponse::from).toList(),
                equipmentRepository.findAllByUserIdOrderByCategoryAscNameAsc(userId).stream()
                        .map(EquipmentResponse::from).toList(),
                backlogRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(BacklogItemResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public String sessionsCsv() {
        String userId = entitlementService.currentUserId();
        StringBuilder csv = new StringBuilder("id,hobby_id,hobby_name,title,started_at,duration_minutes,satisfaction,visibility,notes\n");
        sessionRepository.findAllByUserIdOrderByStartedAtDesc(userId).forEach(session -> csv
                .append(csv(session.getId().toString())).append(',')
                .append(csv(session.getHobby().getId().toString())).append(',')
                .append(csv(session.getHobby().getName())).append(',')
                .append(csv(session.getTitle())).append(',')
                .append(csv(session.getStartedAt().toString())).append(',')
                .append(session.getDurationMinutes()).append(',')
                .append(session.getSatisfaction()).append(',')
                .append(csv(session.getVisibility().value())).append(',')
                .append(csv(session.getNotes())).append('\n'));
        return csv.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String sanitized = value;
        String leadingTrimmed = value.stripLeading();
        if (!leadingTrimmed.isEmpty() && "=+-@".indexOf(leadingTrimmed.charAt(0)) >= 0) {
            sanitized = "'" + value;
        }
        return '"' + sanitized.replace("\"", "\"\"") + '"';
    }
}
