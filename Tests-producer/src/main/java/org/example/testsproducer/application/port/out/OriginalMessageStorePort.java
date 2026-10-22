package org.example.testsproducer.application.port.out;

public interface OriginalMessageStorePort {

    boolean save(String flow, String originalMessage);

    default boolean delete(String flow) {
        return false;
    }
}
