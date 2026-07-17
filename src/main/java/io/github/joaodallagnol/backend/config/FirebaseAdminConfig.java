package io.github.joaodallagnol.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import io.github.joaodallagnol.backend.auth.FirebaseAdminTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.MissingFirebaseConfigurationTokenVerifier;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseAdminConfig {

    private static final String FIREBASE_APP_NAME = "backend-auth";

    @Bean
    FirebaseTokenVerifier firebaseTokenVerifier(
            @Value("${app.auth.firebase.project-id:}") String projectId,
            @Value("${app.auth.firebase.service-account-json-base64:}") String serviceAccountJsonBase64,
            @Value("${app.auth.firebase.service-account-path:}") String serviceAccountPath
    ) {
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
