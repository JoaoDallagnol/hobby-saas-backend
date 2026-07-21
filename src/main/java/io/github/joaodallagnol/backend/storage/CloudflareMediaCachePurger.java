package io.github.joaodallagnol.backend.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CloudflareMediaCachePurger implements MediaCachePurger {

    private final String zoneId;
    private final String apiToken;
    private final String publicBaseUrl;

    public CloudflareMediaCachePurger(
            @Value("${app.integrations.r2.cloudflare-zone-id:}") String zoneId,
            @Value("${app.integrations.r2.cloudflare-api-token:}") String apiToken,
            @Value("${app.integrations.r2.public-base-url:}") String publicBaseUrl
    ) {
        this.zoneId = zoneId;
        this.apiToken = apiToken;
        this.publicBaseUrl = StringUtils.trimTrailingCharacter(publicBaseUrl, '/');
    }

    @Override
    public void purge(String... storageKeys) {
        if (!StringUtils.hasText(zoneId) || !StringUtils.hasText(apiToken)) {
            if (publicBaseUrl.startsWith("http://localhost:") || publicBaseUrl.startsWith("http://127.0.0.1:")) {
                return;
            }
            throw new IllegalStateException("Cloudflare cache purge is not configured.");
        }
        String files = Arrays.stream(storageKeys)
                .map(key -> "\"" + publicBaseUrl + "/" + key.replace("\"", "") + "\"")
                .collect(Collectors.joining(","));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/purge_cache"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"files\":[" + files + "]}"))
                .build();
        try {
            HttpResponse<Void> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Cloudflare cache purge failed.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cloudflare cache purge was interrupted.", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cloudflare cache purge failed.", ex);
        }
    }
}
