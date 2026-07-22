package io.github.joaodallagnol.backend.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GooglePlaceDetailsHttpClient implements GooglePlaceDetailsClient {

    private static final String FIELD_MASK = "id";

    private final String apiKey;
    private final RestClient restClient;

    public GooglePlaceDetailsHttpClient(@Value("${app.integrations.google.places-api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://places.googleapis.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    @Override
    public ResolvedPlace fetchPlaceById(String placeId) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Google Places API key is not configured. Set GOOGLE_PLACES_API_KEY for environment access.");
        }

        try {
            GooglePlaceDetailsResponse response = restClient.get()
                    .uri("/places/{placeId}", placeId)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .retrieve()
                    .body(GooglePlaceDetailsResponse.class);

            if (response == null
                    || !StringUtils.hasText(response.id())) {
                throw new IllegalArgumentException("Google Places returned an incomplete place response.");
            }

            return new ResolvedPlace(response.id());
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to resolve placeId with Google Places.", ex);
        }
    }

    private record GooglePlaceDetailsResponse(String id) {
    }
}
