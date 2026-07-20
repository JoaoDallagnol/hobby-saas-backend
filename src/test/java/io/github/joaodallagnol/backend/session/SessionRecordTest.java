package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRecordTest {

    @Test
    void shouldReplacePlaceEquipmentAndPhotosConsistently() {
        Hobby hobby = hobby("Running");
        SessionRecord session = new SessionRecord(
                "user-1",
                hobby,
                "Morning Run",
                OffsetDateTime.parse("2026-07-19T09:00:00Z"),
                45,
                "Good session",
                4,
                null,
                null,
                Map.of("distance_km", new BigDecimal("8.4"))
        );
        PlaceReference place = place("place-123");
        EquipmentReference firstEquipment = equipment("user-1");
        EquipmentReference secondEquipment = equipment("user-1");

        session.assignPlace(place);
        session.replaceEquipment(Set.of(firstEquipment, secondEquipment));
        session.replacePhotos(List.of("photos/one.webp", "photos/two.webp"));

        assertThat(session.getPlace()).isSameAs(place);
        assertThat(session.getPlaceId()).isEqualTo("place-123");
        assertThat(session.getEquipment()).containsExactlyInAnyOrder(firstEquipment, secondEquipment);
        assertThat(session.getPhotos()).hasSize(2);
        assertThat(session.getPhotos().stream().map(SessionPhoto::getStorageKeyOriginal))
                .containsExactly("photos/one.webp", "photos/two.webp");
    }

    @Test
    void shouldUpdateMutableSessionFields() {
        Hobby originalHobby = hobby("Running");
        Hobby updatedHobby = hobby("Reading");
        SessionRecord session = new SessionRecord(
                "user-1",
                originalHobby,
                "Old title",
                OffsetDateTime.parse("2026-07-19T09:00:00Z"),
                45,
                "Old notes",
                3,
                "place-old",
                null,
                Map.of("distance_km", new BigDecimal("8.4"))
        );
        UUID projectId = UUID.randomUUID();
        Map<String, Object> updatedAttributes = Map.of("pages_read", 32);

        session.update(
                updatedHobby,
                "New title",
                OffsetDateTime.parse("2026-07-20T09:00:00Z"),
                70,
                "New notes",
                5,
                "place-new",
                projectId,
                updatedAttributes
        );

        assertThat(session.getHobby()).isSameAs(updatedHobby);
        assertThat(session.getTitle()).isEqualTo("New title");
        assertThat(session.getStartedAt()).isEqualTo(OffsetDateTime.parse("2026-07-20T09:00:00Z"));
        assertThat(session.getDurationMinutes()).isEqualTo(70);
        assertThat(session.getNotes()).isEqualTo("New notes");
        assertThat(session.getSatisfaction()).isEqualTo(5);
        assertThat(session.getPlaceId()).isEqualTo("place-new");
        assertThat(session.getProjectId()).isEqualTo(projectId);
        assertThat(session.getAttributes()).isEqualTo(updatedAttributes);
    }

    private Hobby hobby(String name) {
        try {
            var hobbyConstructor = Hobby.class.getDeclaredConstructor();
            hobbyConstructor.setAccessible(true);
            Hobby hobby = hobbyConstructor.newInstance();
            setField(Hobby.class, hobby, "id", UUID.randomUUID());
            setField(Hobby.class, hobby, "name", name);

            var categoryConstructor = HobbyCategory.class.getDeclaredConstructor();
            categoryConstructor.setAccessible(true);
            HobbyCategory category = categoryConstructor.newInstance();
            setField(HobbyCategory.class, category, "id", UUID.randomUUID());
            setField(HobbyCategory.class, category, "name", "Sports");
            setField(Hobby.class, hobby, "category", category);
            return hobby;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private PlaceReference place(String placeId) {
        try {
            PlaceReference place = new PlaceReference(placeId, "Park", new BigDecimal("-23.1"), new BigDecimal("-46.2"));
            return place;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private EquipmentReference equipment(String userId) {
        try {
            EquipmentReference equipment = new EquipmentReference(userId, null, "shoe", UUID.randomUUID().toString());
            return equipment;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
