package org.example.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

@ConfigurationProperties("tests-producer.flows")
public record FlowTopicsProperties(Map<String, String> topics) {

    private static final Pattern FLOW_PATTERN = Pattern.compile(
            "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$"
    );
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
            "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$"
    );

    public FlowTopicsProperties {
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException(
                    "tests-producer.flows.topics doit contenir au moins un flux"
            );
        }

        TreeMap<String, String> validatedTopics = new TreeMap<>();
        topics.forEach((flow, topic) -> {
            validateFlow(flow);
            validateTopic(flow, topic);
            validatedTopics.put(flow, topic);
        });
        topics = Collections.unmodifiableMap(validatedTopics);
    }

    public String topicFor(String flow) {
        return topics.get(flow);
    }

    private static void validateFlow(String flow) {
        if (flow == null
                || flow.length() > 255
                || !FLOW_PATTERN.matcher(flow).matches()) {
            throw new IllegalArgumentException(
                    "Nom de flux invalide dans tests-producer.flows.topics: "
                            + flow
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
