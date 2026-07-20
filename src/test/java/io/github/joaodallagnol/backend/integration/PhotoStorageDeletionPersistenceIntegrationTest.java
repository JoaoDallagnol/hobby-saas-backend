package io.github.joaodallagnol.backend.integration;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class PhotoStorageDeletionPersistenceIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID RUNNING_HOBBY_ID = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("""
                DELETE FROM session_photos
                WHERE session_id IN (SELECT id FROM sessions WHERE user_id = 'photo-user')
                """);
        jdbcTemplate.update("DELETE FROM photo_storage_deletions");
        jdbcTemplate.update("DELETE FROM sessions WHERE user_id = 'photo-user'");
        jdbcTemplate.update("DELETE FROM user_hobbies WHERE user_id = 'photo-user'");
        jdbcTemplate.update("DELETE FROM users WHERE id = 'photo-user'");
    }

    @Test
    void queuesBothStorageObjectsWhenPhotoRowIsDeleted() {
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, name, email_verified) VALUES (?, ?, ?, ?)",
                "photo-user", "photo@example.com", "Photo User", true
        );
        jdbcTemplate.update(
                "INSERT INTO user_hobbies (user_id, hobby_id, experience_level) VALUES (?, ?::uuid, ?)",
                "photo-user", RUNNING_HOBBY_ID.toString(), "beginner"
        );
        jdbcTemplate.update(
                """
                INSERT INTO sessions
                    (id, user_id, hobby_id, title, started_at, duration_minutes, satisfaction)
                VALUES (?::uuid, ?, ?::uuid, ?, ?, ?, ?)
                """,
                sessionId.toString(), "photo-user", RUNNING_HOBBY_ID.toString(), "Photo session",
                OffsetDateTime.parse("2026-07-19T12:00:00Z"), 30, 4
        );
        jdbcTemplate.update(
                """
                INSERT INTO session_photos
                    (id, session_id, storage_key_original, storage_key_thumbnail,
                     processing_status, processing_attempts)
                VALUES (?::uuid, ?::uuid, ?, ?, 'ready', 0)
                """,
                photoId.toString(), sessionId.toString(),
                "processed/session-photos/original.webp", "processed/session-photos/thumbnail.webp"
        );

        jdbcTemplate.update("DELETE FROM session_photos WHERE id = ?::uuid", photoId.toString());

        assertThat(jdbcTemplate.queryForList(
                "SELECT storage_key FROM photo_storage_deletions ORDER BY storage_key", String.class
        )).containsExactly(
                "processed/session-photos/original.webp",
                "processed/session-photos/thumbnail.webp"
        );
    }
}
