package org.example.testsproducer.domain.model;

public record Owner(String group, String entity, String name) {
    public Owner {
        group = DomainValue.requireNonBlank(group, "group");
        entity = DomainValue.requireNonBlank(entity, "entity");
        name = DomainValue.requireNonBlank(name, "name");
    }
}
