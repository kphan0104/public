package org.example.testsproducer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class DefaultOriginalMessages {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DefaultOriginalMessages.class
    );

    private final Path directory;

    DefaultOriginalMessages(SwaggerProperties properties) {
        directory = properties.originalMessagesDirectory()
                .toAbsolutePath()
                .normalize();
    }

    Map<String, String> loadFor(Iterable<String> flows) {
        Map<String, String> messages = new LinkedHashMap<>();
        for (String flow : flows) {
            Path messageFile = directory.resolve(flow + ".msg");
            if (!Files.isRegularFile(messageFile)) {
                continue;
            }
            try {
                messages.put(
                        flow,
                        Files.readString(messageFile, StandardCharsets.UTF_8)
                );
            } catch (IOException exception) {
                LOGGER.warn(
                        "Impossible de lire l'originalMessage par défaut {}",
                        messageFile,
                        exception
                );
            }
        }
        return Collections.unmodifiableMap(messages);
    }
}
