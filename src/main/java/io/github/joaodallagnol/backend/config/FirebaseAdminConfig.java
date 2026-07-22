package io.github.joaodallagnol.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import io.github.joaodallagnol.backend.auth.FirebaseAdminTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.auth.LocalDevelopmentTokenVerifier;
import io.github.joaodallagnol.backend.auth.MissingFirebaseConfigurationTokenVerifier;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseAdminConfig {

    private static final String FIREBASE_APP_NAME = "backend-auth";

    @Bean
    FirebaseTokenVerifier firebaseTokenVerifier(
            Environment environment,
            @Value("${app.auth.firebase.project-id:}") String projectId,
            @Value("${app.auth.firebase.service-account-json-base64:}") String serviceAccountJsonBase64,
            @Value("${app.auth.firebase.service-account-path:}") String serviceAccountPath,
            @Value("${app.auth.local.enabled:false}") boolean localAuthEnabled,
            @Value("${app.auth.local.token:}") String localToken,
            @Value("${app.auth.local.user-id:local-dev-user}") String localUserId,
            @Value("${app.auth.local.email:local-dev@example.com}") String localEmail,
            @Value("${app.auth.local.name:Local Dev User}") String localName,
            @Value("${app.auth.local.email-verified:true}") boolean localEmailVerified,
            @Value("${app.auth.local.secondary.token:}") String secondaryToken,
            @Value("${app.auth.local.secondary.user-id:local-secondary-user}") String secondaryUserId,
            @Value("${app.auth.local.secondary.email:local-secondary@example.com}") String secondaryEmail,
            @Value("${app.auth.local.secondary.name:Local Secondary User}") String secondaryName,
            @Value("${app.auth.local.secondary.email-verified:true}") boolean secondaryEmailVerified
    ) {
        if (localAuthEnabled) {
            if (!environment.acceptsProfiles(Profiles.of("local"))) {
                throw new IllegalStateException("Local development auth can only be enabled with the local profile.");
            }
            if (!StringUtils.hasText(localToken)) {
                return new MissingFirebaseConfigurationTokenVerifier("Local development auth is enabled but LOCAL_AUTH_TOKEN is missing.");
            }
            Map<String, FirebaseVerifiedToken> localUsers = new LinkedHashMap<>();
            localUsers.put(localToken.trim(), new FirebaseVerifiedToken(
                    localUserId.trim(), localEmail.trim(), localName.trim(), localEmailVerified));
            if (StringUtils.hasText(secondaryToken)) {
                if (localToken.trim().equals(secondaryToken.trim())) {
                    throw new IllegalStateException("Local development auth tokens must be different.");
                }
                if (localUserId.trim().equals(secondaryUserId.trim())) {
                    throw new IllegalStateException("Local development auth user ids must be different.");
                }
                localUsers.put(secondaryToken.trim(), new FirebaseVerifiedToken(
                        secondaryUserId.trim(), secondaryEmail.trim(), secondaryName.trim(), secondaryEmailVerified));
            }
            return new LocalDevelopmentTokenVerifier(localUsers);
        }

        try {
            GoogleCredentials credentials = resolveCredentials(serviceAccountJsonBase64, serviceAccountPath);
            if (credentials == null) {
                return new MissingFirebaseConfigurationTokenVerifier("Firebase credentials are not configured.");
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            if (StringUtils.hasText(projectId)) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseApp firebaseApp = FirebaseApp.getApps().stream()
                    .filter(app -> FIREBASE_APP_NAME.equals(app.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(optionsBuilder.build(), FIREBASE_APP_NAME));

            return new FirebaseAdminTokenVerifier(FirebaseAuth.getInstance(firebaseApp));
        } catch (Exception ex) {
            return new MissingFirebaseConfigurationTokenVerifier("Firebase credentials are invalid or unavailable.");
        }
    }

    private GoogleCredentials resolveCredentials(String serviceAccountJsonBase64, String serviceAccountPath) throws IOException {
        if (StringUtils.hasText(serviceAccountJsonBase64)) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountJsonBase64);
            try (InputStream inputStream = new ByteArrayInputStream(decoded)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        if (StringUtils.hasText(serviceAccountPath)) {
            try (InputStream inputStream = Files.newInputStream(Path.of(serviceAccountPath))) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        return null;
    }
}
