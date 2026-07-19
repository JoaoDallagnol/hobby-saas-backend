package io.github.joaodallagnol.backend.integration;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class JitUserProvisioningIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM session_equipment");
        jdbcTemplate.update("DELETE FROM session_photos");
        jdbcTemplate.update("DELETE FROM sessions");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM backlog_items");
        jdbcTemplate.update("DELETE FROM user_hobbies");
        jdbcTemplate.update("DELETE FROM hobby_suggestions");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", "jit-user");
    }

    @Test
    void provisionsUserOnFirstAuthenticatedRequest() throws Exception {
        Integer existingUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                "jit-user"
        );

        assertThat(existingUsers).isZero();

        mockMvc.perform(get("/api/me")
                        .header(AUTHORIZATION, "Bearer jit-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("jit-user"))
                .andExpect(jsonPath("$.email").value("jit@example.com"))
                .andExpect(jsonPath("$.name").value("Jit User"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());

        Integer persistedUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                "jit-user"
        );
        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?",
                String.class,
                "jit-user"
        );
        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ?",
                String.class,
                "jit-user"
        );
        Boolean emailVerified = jdbcTemplate.queryForObject(
                "SELECT email_verified FROM users WHERE id = ?",
                Boolean.class,
                "jit-user"
        );
        OffsetDateTime createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM users WHERE id = ?",
                OffsetDateTime.class,
                "jit-user"
        );

        assertThat(persistedUsers).isEqualTo(1);
        assertThat(email).isEqualTo("jit@example.com");
        assertThat(name).isEqualTo("Jit User");
        assertThat(emailVerified).isTrue();
        assertThat(createdAt).isNotNull();
    }
}
