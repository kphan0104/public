package org.example.testsproducer.application.service;

import org.example.testsproducer.adapter.out.json.JacksonEventSerializerAdapter;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.FlowFormat;
import org.example.testsproducer.domain.model.Owner;
import org.example.testsproducer.domain.model.Provider;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
                        "2026-07-28 INFO paiement accepté amount=42",
                        configuredTemplate()
                )
        );

        byte[] payload = publishedPayload.get();
        var json = objectMapper.readTree(payload);
        assertThat(result.topic()).isEqualTo("integration.events");
        assertThat(result.partition()).isEqualTo(2);
        assertThat(result.offset()).isEqualTo(42L);
        assertThat(result.eventSize()).isEqualTo(payload.length);
        assertThat(json.get("databus.flow.name")).isNull();

        var databus = json.get("databus");
        var flow = databus.get("flow");
        var lineage = databus.get("event").get("lineage");
        var stage1 = lineage.get("stage1");

        assertThat(flow.get("name").asText()).isEqualTo("payments");
        assertThat(flow.get("owner").get("group").asText())
                .isEqualTo("configured-group");
        assertThat(flow.get("owner").get("entity").asText())
                .isEqualTo("configured-entity");
        assertThat(flow.get("owner").get("name").asText())
                .isEqualTo("configured-owner");
        assertThat(flow.get("provider").get("name").asText())
                .isEqualTo("configured-provider");
        assertThat(flow.get("provider").get("source").asText())
                .isEqualTo("configured-source");
        assertThat(flow.get("format").get("version").asText())
                .isEqualTo("2.0.0");
        assertThat(flow.get("format").get("type").asText())
                .isEqualTo("NDJSON");
        assertThat(flow.get("retention").asText())
                .isEqualTo("month");
        assertThat(lineage.get("last_stage").asInt()).isEqualTo(1);
        assertThat(stage1.get("location").asText()).isEqualTo("TEST");
        assertThat(stage1.get("pipeline_id").asText())
                .isEqualTo("configured_pipeline");
        assertThat(stage1.get("processing_duration_ms").asInt())
                .isEqualTo(250);
        assertThat(stage1.get("timestamp").asText())
                .isEqualTo("2026-07-24T10:30:15.123Z");
        assertThat(stage1.get("host").asText())
                .isEqualTo("integration-host");
        assertThat(stage1.get("event_size").asInt()).isEqualTo(payload.length);
        assertThat(json.get("originalMessage").asText())
                .isEqualTo("2026-07-28 INFO paiement accepté amount=42");
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
                        "message trop volumineux",
                        configuredTemplate()
                )
        )).isInstanceOf(MessageTooLargeException.class);
    }

    private static DatabusEventTemplate configuredTemplate() {
        return new DatabusEventTemplate(
                new Owner(
                        "configured-group",
                        "configured-entity",
                        "configured-owner"
                ),
                new Provider(
                        "configured-provider",
                        "configured-source"
                ),
                new FlowFormat("2.0.0", "NDJSON"),
                "month",
                "TEST",
                "configured_pipeline",
                250
        );
    }
}
