package org.example.testsproducer.application.port.in;

public record PublishRawMessageResult(
        String topic,
        int partition,
        long offset,
        int messageSize
) {
}
