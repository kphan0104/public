package org.example.testsproducer.domain.model;

import java.util.regex.Pattern;

public record FlowTopicMapping(String flow, String topic) {

    private static final Pattern FLOW_PATTERN = Pattern.compile(
            "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$"
    );
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
            "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$"
    );

    public FlowTopicMapping {
        validateFlow(flow);
        validateTopic(flow, topic);
    }

    public static void validateFlow(String flow) {
        if (flow == null
                || flow.length() > 255
                || !FLOW_PATTERN.matcher(flow).matches()) {
            throw new IllegalArgumentException(
                    "Nom de flux invalide: " + flow
            );
        }
    }

    private static void validateTopic(String flow, String topic) {
        if (topic == null
                || topic.length() > 249
                || !TOPIC_PATTERN.matcher(topic).matches()) {
            throw new IllegalArgumentException(
                    "Topic invalide pour le flux '" + flow + "': " + topic
            );
        }
    }
}
