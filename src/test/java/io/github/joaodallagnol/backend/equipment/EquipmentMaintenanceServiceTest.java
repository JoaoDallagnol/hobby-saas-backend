package io.github.joaodallagnol.backend.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.session.EquipmentReference;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EquipmentMaintenanceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void plusRuleBecomesDueFromActualSessionUsage() {
        EquipmentMaintenanceRuleRepository rules = mock(EquipmentMaintenanceRuleRepository.class);
        EquipmentReferenceRepository equipmentRepository = mock(EquipmentReferenceRepository.class);
        SessionRecordRepository sessions = mock(SessionRecordRepository.class);
        EntitlementService entitlement = mock(EntitlementService.class);
        UUID equipmentId = UUID.randomUUID();
        EquipmentReference equipment = new EquipmentReference("user-1", null, "Shoes", "Daily Trainer");
        when(entitlement.currentUserId()).thenReturn("user-1");
        when(equipmentRepository.findByIdAndUserId(equipmentId, "user-1")).thenReturn(Optional.of(equipment));
        when(rules.save(any(EquipmentMaintenanceRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessions.sumDurationMinutesForEquipment("user-1", equipment.getId())).thenReturn(360L);
        EquipmentMaintenanceService service = new EquipmentMaintenanceService(rules, equipmentRepository, sessions,
                entitlement, CLOCK);

        MaintenanceRuleResponse response = service.create(
                new MaintenanceRuleRequest(equipmentId, "Trocar tênis", 300, null, true));

        assertThat(response.usedMinutes()).isEqualTo(360);
        assertThat(response.remainingMinutes()).isZero();
        assertThat(response.maintenanceDue()).isTrue();
        verify(entitlement).requirePlus("user-1");
    }

    @Test
    void cannotCreateRuleForForeignEquipment() {
        EntitlementService entitlement = mock(EntitlementService.class);
        when(entitlement.currentUserId()).thenReturn("user-1");
        EquipmentMaintenanceService service = new EquipmentMaintenanceService(
                mock(EquipmentMaintenanceRuleRepository.class), mock(EquipmentReferenceRepository.class),
                mock(SessionRecordRepository.class), entitlement, CLOCK);

        assertThatThrownBy(() -> service.create(new MaintenanceRuleRequest(
                UUID.randomUUID(), "Limpeza", 100, OffsetDateTime.parse("2026-07-01T00:00:00Z"), true)))
                .isInstanceOf(io.github.joaodallagnol.backend.session.ResourceNotFoundException.class);
    }
}
