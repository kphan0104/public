package org.example.testsproducer.domain.model;

public record FlowFormat(String version, String type) {
    public FlowFormat {
        version = DomainValue.requireNonBlank(version, "version");
        type = DomainValue.requireNonBlank(type, "type");
    }
}
