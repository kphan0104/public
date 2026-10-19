package org.example.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.example.testsproducer.domain.model.FlowTopicMapping;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@ConfigurationProperties("tests-producer.flows")
public record FlowTopicsProperties(
        Map<String, String> topics,
        Path topicsFile
) {

    public FlowTopicsProperties {
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException(
                    "tests-producer.flows.topics doit contenir au moins un flux"
            );
        }

        TreeMap<String, String> validatedTopics = new TreeMap<>();
        topics.forEach((flow, topic) -> {
            new FlowTopicMapping(flow, topic);
            validatedTopics.put(flow, topic);
        });
        topics = Collections.unmodifiableMap(validatedTopics);

        if (topicsFile == null) {
            topicsFile = Path.of("flow-topics.yml");
        }
        topicsFile = topicsFile.toAbsolutePath().normalize();
    }

}
