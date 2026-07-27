package org.example.testsproducer.domain.model;

import java.util.Objects;

public record Databus(Flow flow, EventMetadata event) {
    public Databus {
        Objects.requireNonNull(flow, "flow");
        Objects.requireNonNull(event, "event");
    }
}
