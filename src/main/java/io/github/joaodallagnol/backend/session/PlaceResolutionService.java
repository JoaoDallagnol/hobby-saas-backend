package io.github.joaodallagnol.backend.session;

import org.springframework.stereotype.Service;

@Service
public class PlaceResolutionService {

    private final PlaceReferenceRepository placeReferenceRepository;
    private final GooglePlaceDetailsClient googlePlaceDetailsClient;

    public PlaceResolutionService(
            PlaceReferenceRepository placeReferenceRepository,
            GooglePlaceDetailsClient googlePlaceDetailsClient
    ) {
        this.placeReferenceRepository = placeReferenceRepository;
        this.googlePlaceDetailsClient = googlePlaceDetailsClient;
    }

    public PlaceReference resolveOrCreate(String placeId) {
        return placeReferenceRepository.findById(placeId)
                .orElseGet(() -> {
                    ResolvedPlace resolvedPlace = googlePlaceDetailsClient.fetchPlaceById(placeId);
                    return placeReferenceRepository.save(new PlaceReference(
                            resolvedPlace.placeId(),
                            resolvedPlace.name(),
                            resolvedPlace.lat(),
                            resolvedPlace.lng()
                    ));
                });
    }
}
