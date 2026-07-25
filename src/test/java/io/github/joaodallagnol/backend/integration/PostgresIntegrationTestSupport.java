package io.github.joaodallagnol.backend.integration;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.session.GooglePlaceDetailsClient;
import io.github.joaodallagnol.backend.session.ResolvedPlace;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("integration")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public abstract class PostgresIntegrationTestSupport {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc(WebApplicationContext webApplicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("app.auth.firebase.project-id", () -> "integration-test-project");
    }

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("hobby_saas_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
        return postgres;
    }

    @TestConfiguration
    static class AuthTestConfig {

        @Bean
        @Primary
        FirebaseTokenVerifier firebaseTokenVerifier() {
            return idToken -> switch (idToken) {
                case "valid-token" -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
                case "other-user-token" -> new FirebaseVerifiedToken("other-user", "other@example.com", "Other User", true);
                case "jit-token" -> new FirebaseVerifiedToken("jit-user", "jit@example.com", "Jit User", true);
                default -> throw new IllegalArgumentException("Token is invalid.");
            };
        }

        @Bean
        @Primary
        GooglePlaceDetailsClient googlePlaceDetailsClient() {
            return placeId -> new ResolvedPlace(placeId);
        }
    }
}
