package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlaceResolutionServiceTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void reusesRecentlyValidatedPlaceIdWithoutProviderCall() {
        InMemoryPlaces places = new InMemoryPlaces();
        places.save(new PlaceReference("place-1", OffsetDateTime.parse("2026-01-01T00:00:00Z")));
        AtomicInteger providerCalls = new AtomicInteger();
        PlaceResolutionService service = new PlaceResolutionService(
                places.repository(),
                placeId -> {
                    providerCalls.incrementAndGet();
                    return new ResolvedPlace(placeId);
                },
                NOW
        );

        PlaceReference result = service.resolveOrCreate("place-1");

        assertThat(result.getPlaceId()).isEqualTo("place-1");
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void revalidatesPlaceIdOlderThanOneYear() {
        InMemoryPlaces places = new InMemoryPlaces();
        places.save(new PlaceReference("place-1", OffsetDateTime.parse("2025-01-01T00:00:00Z")));
        AtomicInteger providerCalls = new AtomicInteger();
        PlaceResolutionService service = new PlaceResolutionService(
                places.repository(),
                placeId -> {
                    providerCalls.incrementAndGet();
                    return new ResolvedPlace(placeId);
                },
                NOW
        );

        PlaceReference result = service.resolveOrCreate("place-1");

        assertThat(providerCalls).hasValue(1);
        assertThat(result.getValidatedAt()).isEqualTo(OffsetDateTime.parse("2026-07-22T12:00:00Z"));
    }

    private static final class InMemoryPlaces {
        private final Map<String, PlaceReference> data = new HashMap<>();

        private PlaceReference save(PlaceReference place) {
            data.put(place.getPlaceId(), place);
            return place;
        }

        private PlaceReferenceRepository repository() {
            return (PlaceReferenceRepository) Proxy.newProxyInstance(
                    PlaceReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{PlaceReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(data.get(args[0]));
                        case "save" -> save((PlaceReference) args[0]);
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "InMemoryPlaces";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
