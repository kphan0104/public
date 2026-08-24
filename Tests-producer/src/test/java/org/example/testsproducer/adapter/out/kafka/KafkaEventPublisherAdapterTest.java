package org.example.testsproducer.adapter.out.kafka;

import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.config.PublicationProperties;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaEventPublisherAdapterTest {

    @Test
    void returnsKafkaMetadataAfterAcknowledgement() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        @SuppressWarnings("unchecked")
        SendResult<String, byte[]> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        byte[] payload = "{}".getBytes();

        when(template.send("events", "payments", payload))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.topic()).thenReturn("events");
        when(metadata.partition()).thenReturn(3);
        when(metadata.offset()).thenReturn(18L);

        var adapter = new KafkaEventPublisherAdapter(
                template,
                new PublicationProperties(Duration.ofSeconds(2), 1_000_000)
        );

        var result = adapter.publish("events", "payments", payload);

        assertThat(result.topic()).isEqualTo("events");
        assertThat(result.partition()).isEqualTo(3);
        assertThat(result.offset()).isEqualTo(18L);
    }

    @Test
    void exposesTheExactErrorMessageReturnedByKafka() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, byte[]> template = mock(KafkaTemplate.class);
        byte[] payload = "{}".getBytes();
        var kafkaException = new TopicAuthorizationException(
                "Not authorized to access topics: [restricted.events]"
        );

        when(template.send("restricted.events", "payments", payload))
                .thenReturn(CompletableFuture.failedFuture(kafkaException));

        var adapter = new KafkaEventPublisherAdapter(
                template,
                new PublicationProperties(Duration.ofSeconds(2), 1_000_000)
        );

        assertThatThrownBy(() -> adapter.publish(
                "restricted.events",
                "payments",
                payload
        ))
                .isInstanceOf(EventPublicationException.class)
                .hasMessage(
                        "Not authorized to access topics: "
                                + "[restricted.events]"
                )
                .hasCause(kafkaException);
    }
}
