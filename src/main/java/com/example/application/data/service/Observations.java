package com.example.application.data.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * Small helper for the demo's custom instrumentation.
 *
 * <p>Under Observability Kit 4 the equivalent was {@code Span.current()}, which always returned
 * something (a no-op span outside a trace). Micrometer's
 * {@link ObservationRegistry#getCurrentObservation()} returns {@code null} when no observation
 * scope is open, so guard it once here instead of null-checking at every call site.
 */
final class Observations {

    private Observations() {
    }

    static Observation current(ObservationRegistry registry) {
        Observation observation = registry.getCurrentObservation();
        return observation != null ? observation : Observation.NOOP;
    }
}
