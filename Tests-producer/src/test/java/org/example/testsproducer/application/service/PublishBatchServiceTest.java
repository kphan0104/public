package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.BatchPublicationException;
import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.application.exception.InvalidVariablesException;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.adapter.out.json.JacksonEventSerializerAdapter;
import org.example.testsproducer.adapter.out.json.JacksonOriginalMessageNormalizerAdapter;
import org.example.testsproducer.application.port.in.PublishBatchCommand;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.FlowFormat;
import org.example.testsproducer.domain.model.MessageVariable;
import org.example.testsproducer.domain.model.Owner;
import org.example.testsproducer.domain.model.Provider;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Map;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishBatchServiceTest {

    @Test
    void generatesEachMessageWithConsistentRepeatedTokensAndLiteralReplacements() {
        var messages = new ArrayList<PublishEventCommand>();
        var service = new PublishBatchService(command -> {
            messages.add(command);
            return null;
        });

        var result = service.publish(command(
                "${USER}/${USER}/${AMOUNT}/${STATUS}/{{NOW}}",
                1000,
                Map.of(
                        "USER", new MessageVariable.StringVariable(8, "a-zA-Z0-9"),
                        "AMOUNT", new MessageVariable.IntegerVariable(-10, 10),
                        "STATUS", new MessageVariable.ChoiceVariable(java.util.List.of("$1\\${USER}"))
                )
        ));

        assertThat(result.publishedCount()).isEqualTo(1000);
        assertThat(messages).hasSize(1000);
        for (var message : messages) {
            var parts = message.originalMessage().split("/", -1);
            assertThat(parts[0]).matches("[a-zA-Z0-9]{8}");
            assertThat(parts[1]).isEqualTo(parts[0]);
            assertThat(Integer.parseInt(parts[2])).isBetween(-10, 10);
            assertThat(parts[3]).isEqualTo("$1\\${USER}");
            assertThat(parts[4]).isEqualTo("{{NOW}}");
            assertThat(message.topic()).isEqualTo("payments.events");
            assertThat(message.flowName()).isEqualTo("payments");
        }
    }

    @Test
    void publishesGeneratedJsonAsAnEscapedStringWithTheExactDatabusSize() {
        var mapper = new ObjectMapper();
        var payloads = new ArrayList<byte[]>();
        var publishEvent = new PublishEventService(
                (topic, key, payload) -> {
                    payloads.add(payload);
                    return new EventPublisherPort.PublishedRecord(topic, 0, payloads.size());
                },
                new JacksonEventSerializerAdapter(mapper),
                () -> "test-host",
                new JacksonOriginalMessageNormalizerAdapter(mapper),
                Clock.systemUTC(),
                1_000_000
        );
        var service = new PublishBatchService(publishEvent);
        service.publish(command("{\n  \"user\":\"${USER}\",\"amount\":${AMOUNT}\n}", 2,
                Map.of("USER", new MessageVariable.ChoiceVariable(java.util.List.of("élise")),
                        "AMOUNT", new MessageVariable.IntegerVariable(42, 42))));

        assertThat(payloads).hasSize(2);
        for (byte[] payload : payloads) {
            var event = mapper.readTree(payload);
            assertThat(event.get("originalMessage").isString()).isTrue();
            assertThat(event.get("originalMessage").asText()).isEqualTo("{\"user\":\"élise\",\"amount\":42}");
            assertThat(event.at("/databus/event/lineage/stage1/event_size").asInt()).isEqualTo(payload.length);
            assertThat(event.at("/databus/flow/provider/name").asText()).isEqualTo("itgp");
        }
    }

    @Test
    void repeatsTheOriginalMessageWhenNoVariablesAreProvided() {
        var messages = new ArrayList<String>();
        var service = new PublishBatchService(command -> {
            messages.add(command.originalMessage());
            return null;
        });
        service.publish(command("message", 3, Map.of()));
        assertThat(messages).containsExactly("message", "message", "message");
    }

    @Test
    void rejectsMissingVariablesBeforeAnyPublication() {
        var service = new PublishBatchService(command -> {
            throw new AssertionError("Aucun message ne doit être publié");
        });
        assertThatThrownBy(() -> service.publish(command("${MISSING}", 2, Map.of())))
                .isInstanceOf(InvalidVariablesException.class)
                .hasMessageContaining("${MISSING}");
        for (int count : new int[]{0, 1001}) {
            assertThatThrownBy(() -> command("message", count, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void stopsOnKafkaFailureAndReportsOnlyAcknowledgedMessages() {
        var attempts = new AtomicInteger();
        var failure = new EventPublicationException("Kafka ACL error", new RuntimeException());
        var service = new PublishBatchService(command -> {
            if (attempts.incrementAndGet() == 3) {
                throw failure;
            }
            return null;
        });
        assertThatThrownBy(() -> service.publish(command("message", 5, Map.of())))
                .isInstanceOfSatisfying(BatchPublicationException.class, exception -> {
                    assertThat(exception.getCause()).isSameAs(failure);
                    assertThat(exception.requestedCount()).isEqualTo(5);
                    assertThat(exception.publishedCount()).isEqualTo(2);
                });
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void reportsOversizedMessagesWithoutLosingBatchProgress() {
        var service = new PublishBatchService(command -> {
            throw new MessageTooLargeException(2000, 1000);
        });
        assertThatThrownBy(() -> service.publish(command("message", 2, Map.of())))
                .isInstanceOfSatisfying(BatchPublicationException.class, exception -> {
                    assertThat(exception.getCause()).isInstanceOf(MessageTooLargeException.class);
                    assertThat(exception.publishedCount()).isZero();
                });
    }

    private PublishBatchCommand command(String message, int count, Map<String, MessageVariable> variables) {
        return new PublishBatchCommand("payments.events", "payments", message, count, variables,
                new DatabusEventTemplate(new Owner("itgp", "itgp", "itgp"),
                        new Provider("itgp", "application"), new FlowFormat("1.0.0", "JSON"),
                        "year", "MN", "integrations_tests", 100));
    }
}
