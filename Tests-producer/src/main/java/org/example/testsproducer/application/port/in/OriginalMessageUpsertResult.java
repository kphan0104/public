package org.example.testsproducer.application.port.in;

public record OriginalMessageUpsertResult(
        String flow,
        boolean created
) {
}
