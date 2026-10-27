package org.example.testsproducer.application.port.in;

import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.MessageVariable;

import java.util.Map;
import java.util.Objects;

public record PublishBatchCommand(
        String topic,
        String flowName,
        String originalMessage,
        int count,
        Map<String, MessageVariable> variables,
        DatabusEventTemplate eventTemplate
) {
    public PublishBatchCommand {
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(flowName, "flowName");
        Objects.requireNonNull(originalMessage, "originalMessage");
        Objects.requireNonNull(eventTemplate, "eventTemplate");
        variables = Map.copyOf(variables);
        if (count < 1 || count > 1000) {
            throw new IllegalArgumentException("count doit être compris entre 1 et 1000");
        }
    }
}
