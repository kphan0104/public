package org.example.testsproducer.application.port.out;

public interface EventPublisherPort {

    PublishedRecord publish(String topic, String key, byte[] payload);

    record PublishedRecord(String topic, int partition, long offset) {
    }
}
