package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishRawMessageResult;

public record PublishRawMessageResponse(
        String status,
        String topic,
        int partition,
        long offset,
        int messageSize
) {
    static PublishRawMessageResponse from(PublishRawMessageResult result) {
        return new PublishRawMessageResponse(
                "published",
                result.topic(),
                result.partition(),
                result.offset(),
                result.messageSize()
        );
    }
}
