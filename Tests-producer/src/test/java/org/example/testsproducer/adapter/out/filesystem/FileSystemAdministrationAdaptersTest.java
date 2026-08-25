package org.example.testsproducer.adapter.out.filesystem;

import org.example.testsproducer.config.AdminProperties;
import org.example.testsproducer.config.SwaggerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemAdministrationAdaptersTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void rewritesTheFlowTopicsFileInSortedOrder() throws Exception {
        Path flowTopicsFile = temporaryDirectory.resolve("flow-topics.yml");
        var properties = new AdminProperties(
                "01234567890123456789012345678901",
                flowTopicsFile
        );
        var adapter = new FileSystemFlowTopicsStoreAdapter(properties);

        adapter.save(Map.of(
                "payments", "payments.events",
                "orders", "orders.events"
        ));

        assertThat(Files.readString(flowTopicsFile)).isEqualTo("""
                tests-producer:
                  flows:
                    topics:
                      orders: orders.events
                      payments: payments.events
                """);
    }

    @Test
    void createsThenUpdatesAnOriginalMessage() throws Exception {
        Path messagesDirectory = temporaryDirectory.resolve(
                "original-messages"
        );
        var adapter = new FileSystemOriginalMessageStoreAdapter(
                new SwaggerProperties(messagesDirectory)
        );

        assertThat(adapter.save("payments", "premier message")).isTrue();
        assertThat(adapter.save("payments", "{{NOW}} second message"))
                .isFalse();
        assertThat(Files.readString(messagesDirectory.resolve("payments.msg")))
                .isEqualTo("{{NOW}} second message");
    }
}
