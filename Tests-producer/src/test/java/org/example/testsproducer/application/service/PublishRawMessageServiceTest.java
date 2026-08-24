package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.port.in.PublishRawMessageCommand;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishRawMessageServiceTest {

    @Test
    void publishesTheExactBytesWithoutKafkaKey() {
        byte[] rawMessage = new byte[]{0, 1, -1, 10, 13, 65};
        AtomicReference<byte[]> publishedPayload = new AtomicReference<>();
        EventPublisherPort publisher = (topic, key, payload) -> {
            assertThat(topic).isEqualTo("raw.events");
            assertThat(key).isNull();
            publishedPayload.set(payload);
            return new EventPublisherPort.PublishedRecord(topic, 3, 42L);
        };
        var service = new PublishRawMessageService(publisher, 100);

        var result = service.publish(
                new PublishRawMessageCommand("raw.events", rawMessage)
        );

        assertThat(publishedPayload.get()).isEqualTo(rawMessage);
        assertThat(result.topic()).isEqualTo("raw.events");
        assertThat(result.partition()).isEqualTo(3);
        assertThat(result.offset()).isEqualTo(42L);
        assertThat(result.messageSize()).isEqualTo(rawMessage.length);
    }

    @Test
    void rejectsARawMessageAboveTheConfiguredLimit() {
        var service = new PublishRawMessageService(
                (topic, key, payload) -> {
                    throw new AssertionError("Kafka ne doit pas être appelé");
                },
                2
        );

        assertThatThrownBy(() -> service.publish(
                new PublishRawMessageCommand(
                        "raw.events",
                        new byte[]{1, 2, 3}
                )
        )).isInstanceOf(MessageTooLargeException.class);
    }
}
