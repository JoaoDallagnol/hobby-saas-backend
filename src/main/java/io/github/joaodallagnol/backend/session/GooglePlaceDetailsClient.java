package io.github.joaodallagnol.backend.session;

public interface GooglePlaceDetailsClient {

    ResolvedPlace fetchPlaceById(String placeId);
}
