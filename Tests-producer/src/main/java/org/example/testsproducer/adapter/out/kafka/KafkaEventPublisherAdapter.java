package org.example.testsproducer.adapter.out.kafka;

import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.config.PublicationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final PublicationProperties properties;

    public KafkaEventPublisherAdapter(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            PublicationProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public PublishedRecord publish(
            String topic,
            String key,
            byte[] payload
    ) {
        try {
            var sendResult = kafkaTemplate
                    .send(topic, key, payload)
                    .get(
                            properties.acknowledgementTimeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    );
            var metadata = sendResult.getRecordMetadata();
            return new PublishedRecord(
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublicationException(
                    "Publication Kafka interrompue",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new EventPublicationException(
                    "Kafka n'a pas acquitté le message",
                    exception
            );
        }
    }
}
