package io.github.joaodallagnol.backend.equipment;

import io.github.joaodallagnol.backend.session.EquipmentReference;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EquipmentMaintenanceService {
    private final EquipmentMaintenanceRuleRepository ruleRepository;
    private final EquipmentReferenceRepository equipmentRepository;
    private final SessionRecordRepository sessionRepository;
    private final EntitlementService entitlementService;
    private final Clock clock;

    public EquipmentMaintenanceService(EquipmentMaintenanceRuleRepository ruleRepository,
                                       EquipmentReferenceRepository equipmentRepository,
                                       SessionRecordRepository sessionRepository,
                                       EntitlementService entitlementService, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.equipmentRepository = equipmentRepository;
        this.sessionRepository = sessionRepository;
        this.entitlementService = entitlementService;
        this.clock = clock;
    }

    public List<MaintenanceRuleResponse> list() {
        String userId = entitlementService.currentUserId();
        return ruleRepository.findAllByEquipmentUserIdOrderByCreatedAtDesc(userId).stream()
                .map(rule -> response(userId, rule)).toList();
    }

    @Transactional
    public MaintenanceRuleResponse create(MaintenanceRuleRequest request) {
        String userId = entitlementService.currentUserId();
        entitlementService.requirePlus(userId);
        EquipmentReference equipment = ownedEquipment(request.equipmentId(), userId);
        validateDate(request.lastMaintainedAt());
        EquipmentMaintenanceRule rule = new EquipmentMaintenanceRule(equipment, request.name().trim(),
                request.intervalMinutes(), request.lastMaintainedAt(), OffsetDateTime.now(clock));
        if (Boolean.FALSE.equals(request.active())) {
            rule.update(rule.getName(), rule.getIntervalMinutes(), rule.getLastMaintainedAt(), false);
        }
        return response(userId, ruleRepository.save(rule));
    }

    @Transactional
    public MaintenanceRuleResponse update(UUID ruleId, MaintenanceRuleRequest request) {
        String userId = entitlementService.currentUserId();
        entitlementService.requirePlus(userId);
        EquipmentMaintenanceRule rule = ownedRule(ruleId, userId);
        if (request.equipmentId() != null && !request.equipmentId().equals(rule.getEquipment().getId())) {
            throw new IllegalArgumentException("equipmentId cannot be changed.");
        }
        validateDate(request.lastMaintainedAt());
        rule.update(request.name().trim(), request.intervalMinutes(), request.lastMaintainedAt(),
                request.active() == null ? rule.isActive() : request.active());
        return response(userId, rule);
    }

    @Transactional
    public MaintenanceRuleResponse markMaintained(UUID ruleId) {
        String userId = entitlementService.currentUserId();
        entitlementService.requirePlus(userId);
        EquipmentMaintenanceRule rule = ownedRule(ruleId, userId);
        rule.markMaintained(OffsetDateTime.now(clock));
        return response(userId, rule);
    }

    @Transactional
    public void delete(UUID ruleId) {
        String userId = entitlementService.currentUserId();
        ruleRepository.delete(ownedRule(ruleId, userId));
    }

    private MaintenanceRuleResponse response(String userId, EquipmentMaintenanceRule rule) {
        long used = rule.getLastMaintainedAt() == null
                ? sessionRepository.sumDurationMinutesForEquipment(userId, rule.getEquipment().getId())
                : sessionRepository.sumDurationMinutesForEquipmentSince(userId, rule.getEquipment().getId(),
                        rule.getLastMaintainedAt());
        long remaining = Math.max(0, rule.getIntervalMinutes() - used);
        return new MaintenanceRuleResponse(rule.getId(), rule.getEquipment().getId(), rule.getEquipment().getName(),
                rule.getName(), rule.getIntervalMinutes(), rule.getLastMaintainedAt(), rule.isActive(), used, remaining,
                rule.isActive() && used >= rule.getIntervalMinutes());
    }

    private EquipmentMaintenanceRule ownedRule(UUID id, String userId) {
        return ruleRepository.findByIdAndEquipmentUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance rule not found."));
    }

    private EquipmentReference ownedEquipment(UUID id, String userId) {
        if (id == null) throw new IllegalArgumentException("equipmentId is required.");
        return equipmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found."));
    }

    private void validateDate(OffsetDateTime value) {
        if (value != null && value.isAfter(OffsetDateTime.now(clock))) {
            throw new IllegalArgumentException("lastMaintainedAt cannot be in the future.");
        }
    }
}
