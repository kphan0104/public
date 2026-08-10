package org.example.testsproducer.application.port.in;

import org.example.testsproducer.domain.model.DatabusEventTemplate;

import java.util.Objects;

public record PublishEventCommand(
        String topic,
        String flowName,
        String originalMessage,
        DatabusEventTemplate eventTemplate
) {
    public PublishEventCommand {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(flowName, "flowName");
        Objects.requireNonNull(originalMessage, "originalMessage");
        Objects.requireNonNull(eventTemplate, "eventTemplate");
    }
}
