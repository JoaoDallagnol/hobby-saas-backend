package io.github.joaodallagnol.backend.session;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceResolutionService {

    private final PlaceReferenceRepository placeReferenceRepository;
    private final GooglePlaceDetailsClient googlePlaceDetailsClient;
    private final Clock clock;

    @Autowired
    public PlaceResolutionService(
            PlaceReferenceRepository placeReferenceRepository,
            GooglePlaceDetailsClient googlePlaceDetailsClient,
            Clock clock
    ) {
        this.placeReferenceRepository = placeReferenceRepository;
        this.googlePlaceDetailsClient = googlePlaceDetailsClient;
        this.clock = clock;
    }

    public PlaceResolutionService(PlaceReferenceRepository placeReferenceRepository,
                                  GooglePlaceDetailsClient googlePlaceDetailsClient) {
        this(placeReferenceRepository, googlePlaceDetailsClient, Clock.systemUTC());
    }

    public PlaceReference resolveOrCreate(String placeId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        PlaceReference cached = placeReferenceRepository.findById(placeId).orElse(null);
        if (cached != null && cached.getValidatedAt().isAfter(now.minus(365, ChronoUnit.DAYS))) {
            return cached;
        }

        ResolvedPlace resolvedPlace = googlePlaceDetailsClient.fetchPlaceById(placeId);
        if (cached != null && resolvedPlace.placeId().equals(cached.getPlaceId())) {
            cached.markValidatedAt(now);
            return placeReferenceRepository.save(cached);
        }
        return placeReferenceRepository.save(new PlaceReference(resolvedPlace.placeId(), now));
    }
}
