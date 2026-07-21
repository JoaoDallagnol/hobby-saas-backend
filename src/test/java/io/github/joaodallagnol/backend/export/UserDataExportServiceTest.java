package io.github.joaodallagnol.backend.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.ProductUserRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class UserDataExportServiceTest {

    @Test
    void csvEscapesQuotesAndNeutralizesSpreadsheetFormulas() {
        EntitlementService entitlement = mock(EntitlementService.class);
        SessionRecordRepository sessions = mock(SessionRecordRepository.class);
        Hobby hobby = BeanUtils.instantiateClass(Hobby.class);
        ReflectionTestUtils.setField(hobby, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(hobby, "name", "Photography");
        SessionRecord session = new SessionRecord("user-1", hobby, "=HYPERLINK(\"bad\")",
                OffsetDateTime.parse("2026-07-21T10:00:00Z"), 30, "@SUM(1+1)", 4,
                null, null, Map.of());
        when(entitlement.currentUserId()).thenReturn("user-1");
        when(sessions.findAllByUserIdOrderByStartedAtDesc("user-1")).thenReturn(List.of(session));
        UserDataExportService service = new UserDataExportService(entitlement, mock(ProductUserRepository.class),
                mock(UserHobbyRepository.class), sessions, mock(EquipmentReferenceRepository.class),
                mock(BacklogItemReferenceRepository.class), Clock.systemUTC());

        String csv = service.sessionsCsv();

        assertThat(csv).contains("\"'=HYPERLINK(\"\"bad\"\")\"");
        assertThat(csv).contains("\"'@SUM(1+1)\"");
    }
}
