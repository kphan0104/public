package org.example.testsproducer.domain.model;

import java.util.Objects;

public record EventMetadata(Lineage lineage) {
    public EventMetadata {
        Objects.requireNonNull(lineage, "lineage");
    }
}
