package fr.itgp.testsproducer.application.port.in;

import java.util.Objects;

public record PublishEventCommand(
        String topic,
        String flowName,
        Object originalMessage
) {
    public PublishEventCommand {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(flowName, "flowName");
        Objects.requireNonNull(originalMessage, "originalMessage");
    }
}
