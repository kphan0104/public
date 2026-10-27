package org.example.testsproducer.application.port.in;

public record PublishBatchResult(String topic, int requestedCount, int publishedCount) {
}
