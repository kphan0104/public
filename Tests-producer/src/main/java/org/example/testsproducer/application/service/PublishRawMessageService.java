package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.port.in.PublishRawMessageCommand;
import org.example.testsproducer.application.port.in.PublishRawMessageResult;
import org.example.testsproducer.application.port.in.PublishRawMessageUseCase;
import org.example.testsproducer.application.port.out.EventPublisherPort;

public final class PublishRawMessageService
        implements PublishRawMessageUseCase {

    private final EventPublisherPort eventPublisher;
    private final int maxMessageBytes;

    public PublishRawMessageService(
            EventPublisherPort eventPublisher,
            int maxMessageBytes
    ) {
        this.eventPublisher = eventPublisher;
        this.maxMessageBytes = maxMessageBytes;
    }

    @Override
    public PublishRawMessageResult publish(PublishRawMessageCommand command) {
        byte[] payload = command.rawMessage();
        if (payload.length > maxMessageBytes) {
            throw new MessageTooLargeException(
                    payload.length,
                    maxMessageBytes
            );
        }

        EventPublisherPort.PublishedRecord published = eventPublisher.publish(
                command.topic(),
                null,
                payload
        );
        return new PublishRawMessageResult(
                published.topic(),
                published.partition(),
                published.offset(),
                payload.length
        );
    }
}
