package org.example.testsproducer.domain.model;

import java.util.Objects;

public record Flow(
        String name,
        Owner owner,
        Provider provider,
        FlowFormat format,
        String retention
) {
    public Flow {
        name = DomainValue.requireNonBlank(name, "name");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(format, "format");
        retention = DomainValue.requireNonBlank(retention, "retention");
    }
}
