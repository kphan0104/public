package org.example.testsproducer.application.port.in;

public record FlowUpsertResult(
        String flow,
        String topic,
        boolean created
) {
}
