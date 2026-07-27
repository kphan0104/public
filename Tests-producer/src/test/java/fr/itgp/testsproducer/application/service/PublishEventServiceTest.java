package fr.itgp.testsproducer.application.service;

import fr.itgp.testsproducer.adapter.out.json.JacksonEventSerializerAdapter;
import fr.itgp.testsproducer.application.exception.MessageTooLargeException;
import fr.itgp.testsproducer.application.port.in.PublishEventCommand;
import fr.itgp.testsproducer.application.port.out.EventPublisherPort;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishEventServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesTheCompleteDatabusEventWithItsExactUtf8Size()
            throws Exception {
        AtomicReference<byte[]> publishedPayload = new AtomicReference<>();
        EventPublisherPort publisher = (topic, key, payload) -> {
            assertThat(topic).isEqualTo("integration.events");
            assertThat(key).isEqualTo("payments");
            publishedPayload.set(payload);
            return new EventPublisherPort.PublishedRecord(topic, 2, 42L);
        };
        var service = new PublishEventService(
                publisher,
                new JacksonEventSerializerAdapter(objectMapper),
                () -> "integration-host",
                Clock.fixed(
                        Instant.parse("2026-07-24T10:30:15.123Z"),
                        ZoneOffset.UTC
                ),
                1_000_000
        );

        var result = service.publish(
                new PublishEventCommand(
                        "integration.events",
                        "payments",
                        Map.of(
                                "message",
                                "paiement accepté",
                                "amount",
                                42
                        )
                )
        );

        byte[] payload = publishedPayload.get();
        var json = objectMapper.readTree(payload);
        assertThat(result.topic()).isEqualTo("integration.events");
        assertThat(result.partition()).isEqualTo(2);
        assertThat(result.offset()).isEqualTo(42L);
        assertThat(result.eventSize()).isEqualTo(payload.length);
        assertThat(json.get("databus.flow.name").asText())
                .isEqualTo("payments");
        assertThat(json.get("databus.flow.owner.group").asText())
                .isEqualTo("itgp");
        assertThat(json.get("databus.flow.provider.source").asText())
                .isEqualTo("application");
        assertThat(json.get("databus.event.lineage.last_stage").asInt())
                .isEqualTo(1);
        assertThat(
                json.get("databus.event.lineage.stage1.timestamp").asText()
        ).isEqualTo("2026-07-24T10:30:15.123Z");
        assertThat(json.get("databus.event.lineage.stage1.host").asText())
                .isEqualTo("integration-host");
        assertThat(
                json.get("databus.event.lineage.stage1.event_size").asInt()
        ).isEqualTo(payload.length);
        assertThat(json.get("originalMessage").get("amount").asInt())
                .isEqualTo(42);
    }

    @Test
    void rejectsAMessageAboveTheConfiguredLimit() {
        var service = new PublishEventService(
                (topic, key, payload) -> {
                    throw new AssertionError("Kafka ne doit pas être appelé");
                },
                new JacksonEventSerializerAdapter(objectMapper),
                () -> "host",
                Clock.systemUTC(),
                10
        );

        assertThatThrownBy(() -> service.publish(
                new PublishEventCommand(
                        "events",
                        "payments",
                        "message trop volumineux"
                )
        )).isInstanceOf(MessageTooLargeException.class);
    }
}
