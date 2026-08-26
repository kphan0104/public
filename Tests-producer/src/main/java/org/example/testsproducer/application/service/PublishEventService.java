package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventResult;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.application.port.out.EventSerializerPort;
import org.example.testsproducer.application.port.out.HostnameProviderPort;
import org.example.testsproducer.application.port.out.OriginalMessageNormalizerPort;
import org.example.testsproducer.domain.model.DatabusEvent;

import java.time.Clock;

public final class PublishEventService implements PublishEventUseCase {

    private static final int MAX_SIZE_CALCULATION_ATTEMPTS = 10;

    private final EventPublisherPort eventPublisher;
    private final EventSerializerPort eventSerializer;
    private final HostnameProviderPort hostnameProvider;
    private final OriginalMessageNormalizerPort originalMessageNormalizer;
    private final Clock clock;
    private final int maxMessageBytes;

    public PublishEventService(
            EventPublisherPort eventPublisher,
            EventSerializerPort eventSerializer,
            HostnameProviderPort hostnameProvider,
            OriginalMessageNormalizerPort originalMessageNormalizer,
            Clock clock,
            int maxMessageBytes
    ) {
        this.eventPublisher = eventPublisher;
        this.eventSerializer = eventSerializer;
        this.hostnameProvider = hostnameProvider;
        this.originalMessageNormalizer = originalMessageNormalizer;
        this.clock = clock;
        this.maxMessageBytes = maxMessageBytes;
    }

    @Override
    public PublishEventResult publish(PublishEventCommand command) {
        DatabusEvent event = command.eventTemplate().create(
                command.flowName(),
                originalMessageNormalizer.normalize(
                        command.originalMessage()
                ),
                clock.instant(),
                hostnameProvider.hostname()
        );

        EncodedEvent encodedEvent = encodeWithFinalSize(event);
        if (encodedEvent.payload().length > maxMessageBytes) {
            throw new MessageTooLargeException(
                    encodedEvent.payload().length,
                    maxMessageBytes
            );
        }

        EventPublisherPort.PublishedRecord published = eventPublisher.publish(
                command.topic(),
                command.flowName(),
                encodedEvent.payload()
        );

        return new PublishEventResult(
                published.topic(),
                published.partition(),
                published.offset(),
                encodedEvent.event().eventSize(),
                encodedEvent.event().timestamp()
        );
    }

    private EncodedEvent encodeWithFinalSize(DatabusEvent initialEvent) {
        DatabusEvent event = initialEvent;
        for (int attempt = 0;
                attempt < MAX_SIZE_CALCULATION_ATTEMPTS;
                attempt++) {
            byte[] payload = eventSerializer.serialize(event);
            if (event.eventSize() == payload.length) {
                return new EncodedEvent(event, payload);
            }
            event = event.withEventSize(payload.length);
        }
        throw new IllegalStateException(
                "Impossible de stabiliser la taille du message JSON"
        );
    }

    private record EncodedEvent(DatabusEvent event, byte[] payload) {
    }
}
