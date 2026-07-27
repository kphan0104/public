package org.example.testsproducer.domain.model;

public record Provider(String name, String source) {
    public Provider {
        name = DomainValue.requireNonBlank(name, "name");
        source = DomainValue.requireNonBlank(source, "source");
    }
}
