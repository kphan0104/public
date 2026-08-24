package org.example.testsproducer.application.port.in;

import java.util.Objects;

public record PublishRawMessageCommand(String topic, byte[] rawMessage) {

    public PublishRawMessageCommand {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(rawMessage, "rawMessage");
        rawMessage = rawMessage.clone();
    }

    @Override
    public byte[] rawMessage() {
        return rawMessage.clone();
    }
}
