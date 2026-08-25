package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.AdministrationStorageException;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.exception.UnknownConfiguredFlowException;
import org.example.testsproducer.application.port.out.FlowTopicsStorePort;
import org.example.testsproducer.application.port.out.OriginalMessageStorePort;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowAdministrationServiceTest {

    @Test
    void createsAndUpdatesAFlowAfterPersistence() {
        AtomicReference<Map<String, String>> persisted =
                new AtomicReference<>();
        var service = service(persisted::set, (flow, message) -> true, 1000);

        var creation = service.upsertFlow("orders", "orders.events");
        assertThat(creation.created()).isTrue();
        assertThat(service.topicFor("orders")).isEqualTo("orders.events");
        assertThat(persisted.get()).containsEntry(
                "orders",
                "orders.events"
        );

        var update = service.upsertFlow("payments", "payments.v2");
        assertThat(update.created()).isFalse();
        assertThat(service.topicFor("payments")).isEqualTo("payments.v2");
    }

    @Test
    void keepsTheRuntimeRegistryUnchangedWhenPersistenceFails() {
        FlowTopicsStorePort failingStore = topics -> {
            throw new AdministrationStorageException(
                    "échec",
                    new IllegalStateException("disque indisponible")
            );
        };
        var service = service(
                failingStore,
                (flow, message) -> true,
                1000
        );

        assertThatThrownBy(() -> service.upsertFlow("orders", "orders.events"))
                .isInstanceOf(AdministrationStorageException.class);
        assertThat(service.topicFor("orders")).isNull();
        assertThat(service.topicFor("payments"))
                .isEqualTo("integration.events");
    }

    @Test
    void createsAnOriginalMessageOnlyForAKnownFlow() {
        AtomicReference<String> storedMessage = new AtomicReference<>();
        OriginalMessageStorePort messageStore = (flow, message) -> {
            assertThat(flow).isEqualTo("payments");
            storedMessage.set(message);
            return true;
        };
        var service = service(topics -> { }, messageStore, 1000);

        var result = service.upsertOriginalMessage(
                "payments",
                "{{NOW}} message"
        );
        assertThat(result.created()).isTrue();
        assertThat(storedMessage.get()).isEqualTo("{{NOW}} message");

        assertThatThrownBy(() -> service.upsertOriginalMessage(
                "unknown",
                "message"
        )).isInstanceOf(UnknownConfiguredFlowException.class);
    }

    @Test
    void rejectsAnOriginalMessageAboveTheConfiguredLimit() {
        var service = service(
                topics -> { },
                (flow, message) -> {
                    throw new AssertionError("Le fichier ne doit pas être écrit");
                },
                5
        );

        assertThatThrownBy(() -> service.upsertOriginalMessage(
                "payments",
                "message"
        )).isInstanceOf(MessageTooLargeException.class);
    }

    private FlowAdministrationService service(
            FlowTopicsStorePort topicsStore,
            OriginalMessageStorePort messageStore,
            int maxMessageBytes
    ) {
        return new FlowAdministrationService(
                Map.of("payments", "integration.events"),
                topicsStore,
                messageStore,
                maxMessageBytes
        );
    }
}
