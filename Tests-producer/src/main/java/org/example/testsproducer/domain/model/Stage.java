package org.example.testsproducer.domain.model;

import java.time.Instant;
import java.util.Objects;

public record Stage(
        Instant timestamp,
        String pipelineId,
        String host,
        int eventSize,
        String location,
        int processingDurationMs
) {
    public Stage {
        Objects.requireNonNull(timestamp, "timestamp");
        pipelineId = DomainValue.requireNonBlank(pipelineId, "pipelineId");
        host = DomainValue.requireNonBlank(host, "host");
        location = DomainValue.requireNonBlank(location, "location");
        if (eventSize < 0) {
            throw new IllegalArgumentException(
                    "eventSize ne peut pas être négatif"
            );
        }
        if (processingDurationMs < 0) {
            throw new IllegalArgumentException(
                    "processingDurationMs ne peut pas être négatif"
            );
        }
    }

    public Stage withEventSize(int size) {
        return new Stage(
                timestamp,
                pipelineId,
                host,
                size,
                location,
                processingDurationMs
        );
    }
}
