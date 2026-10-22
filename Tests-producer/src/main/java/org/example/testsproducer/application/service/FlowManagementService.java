package org.example.testsproducer.application.service;

import org.example.testsproducer.application.exception.LastConfiguredFlowDeletionException;
import org.example.testsproducer.application.exception.MessageTooLargeException;
import org.example.testsproducer.application.exception.UnknownConfiguredFlowException;
import org.example.testsproducer.application.port.in.FlowManagementUseCase;
import org.example.testsproducer.application.port.in.FlowUpsertResult;
import org.example.testsproducer.application.port.in.OriginalMessageUpsertResult;
import org.example.testsproducer.application.port.out.FlowTopicsStorePort;
import org.example.testsproducer.application.port.out.OriginalMessageStorePort;
import org.example.testsproducer.domain.model.FlowTopicMapping;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class FlowManagementService implements FlowManagementUseCase {

    private final FlowTopicsStorePort flowTopicsStore;
    private final OriginalMessageStorePort originalMessageStore;
    private final int maxMessageBytes;
    private final ReentrantLock updateLock = new ReentrantLock();
    private final AtomicReference<Map<String, String>> topics;

    public FlowManagementService(
            Map<String, String> initialTopics,
            FlowTopicsStorePort flowTopicsStore,
            OriginalMessageStorePort originalMessageStore,
            int maxMessageBytes
    ) {
        this.flowTopicsStore = flowTopicsStore;
        this.originalMessageStore = originalMessageStore;
        this.maxMessageBytes = maxMessageBytes;
        this.topics = new AtomicReference<>(immutableTopics(initialTopics));
    }

    @Override
    public String topicFor(String flow) {
        return topics.get().get(flow);
    }

    @Override
    public Map<String, String> topics() {
        return topics.get();
    }

    @Override
    public FlowUpsertResult upsertFlow(String flow, String topic) {
        String normalizedFlow = flow.trim();
        String normalizedTopic = topic.trim();
        new FlowTopicMapping(normalizedFlow, normalizedTopic);

        updateLock.lock();
        try {
            Map<String, String> currentTopics = topics.get();
            boolean created = !currentTopics.containsKey(normalizedFlow);
            TreeMap<String, String> updatedTopics = new TreeMap<>(
                    currentTopics
            );
            updatedTopics.put(normalizedFlow, normalizedTopic);
            Map<String, String> immutableTopics = Collections.unmodifiableMap(
                    updatedTopics
            );
            flowTopicsStore.save(immutableTopics);
            topics.set(immutableTopics);
            return new FlowUpsertResult(
                    normalizedFlow,
                    normalizedTopic,
                    created
            );
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void deleteFlow(String flow) {
        String normalizedFlow = flow.trim();
        FlowTopicMapping.validateFlow(normalizedFlow);

        updateLock.lock();
        try {
            Map<String, String> currentTopics = topics.get();
            if (!currentTopics.containsKey(normalizedFlow)) {
                throw new UnknownConfiguredFlowException(normalizedFlow);
            }
            if (currentTopics.size() == 1) {
                throw new LastConfiguredFlowDeletionException();
            }

            TreeMap<String, String> updatedTopics = new TreeMap<>(
                    currentTopics
            );
            updatedTopics.remove(normalizedFlow);
            Map<String, String> immutableTopics = Collections.unmodifiableMap(
                    updatedTopics
            );
            flowTopicsStore.save(immutableTopics);
            topics.set(immutableTopics);
            originalMessageStore.delete(normalizedFlow);
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public OriginalMessageUpsertResult upsertOriginalMessage(
            String flow,
            String originalMessage
    ) {
        String normalizedFlow = flow.trim();
        FlowTopicMapping.validateFlow(normalizedFlow);
        int messageSize = originalMessage.getBytes(
                StandardCharsets.UTF_8
        ).length;
        if (messageSize > maxMessageBytes) {
            throw new MessageTooLargeException(
                    messageSize,
                    maxMessageBytes
            );
        }
        updateLock.lock();
        try {
            if (!topics.get().containsKey(normalizedFlow)) {
                throw new UnknownConfiguredFlowException(normalizedFlow);
            }
            boolean created = originalMessageStore.save(
                    normalizedFlow,
                    originalMessage
            );
            return new OriginalMessageUpsertResult(normalizedFlow, created);
        } finally {
            updateLock.unlock();
        }
    }

    private static Map<String, String> immutableTopics(
            Map<String, String> source
    ) {
        TreeMap<String, String> validatedTopics = new TreeMap<>();
        source.forEach((flow, topic) -> {
            new FlowTopicMapping(flow, topic);
            validatedTopics.put(flow, topic);
        });
        return Collections.unmodifiableMap(validatedTopics);
    }
}
