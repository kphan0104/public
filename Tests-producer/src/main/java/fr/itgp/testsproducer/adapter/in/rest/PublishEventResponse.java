package fr.itgp.testsproducer.adapter.in.rest;

import fr.itgp.testsproducer.application.port.in.PublishEventResult;

public record PublishEventResponse(
        String status,
        String topic,
        int partition,
        long offset,
        int eventSize,
        String timestamp
) {
    static PublishEventResponse from(PublishEventResult result) {
        return new PublishEventResponse(
                "published",
                result.topic(),
                result.partition(),
                result.offset(),
                result.eventSize(),
                result.timestamp()
        );
    }
}
