package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishBatchResult;

public record PublishBatchResponse(String status, String topic, int requestedCount, int publishedCount) {

    static PublishBatchResponse from(PublishBatchResult result) {
        return new PublishBatchResponse("published", result.topic(), result.requestedCount(), result.publishedCount());
    }
}
