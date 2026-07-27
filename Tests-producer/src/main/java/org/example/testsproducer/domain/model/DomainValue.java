package org.example.testsproducer.domain.model;

final class DomainValue {

    private DomainValue() {
    }

    static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " ne peut pas être vide"
            );
        }
        return value;
    }
}
