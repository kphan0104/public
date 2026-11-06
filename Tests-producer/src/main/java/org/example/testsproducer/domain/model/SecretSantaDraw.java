package org.example.testsproducer.domain.model;

import java.util.List;

public record SecretSantaDraw(List<Entry> entries) {
    public SecretSantaDraw {
        entries = List.copyOf(entries);
    }

    public record Entry(String participant, String token, String recipient) {}
}
