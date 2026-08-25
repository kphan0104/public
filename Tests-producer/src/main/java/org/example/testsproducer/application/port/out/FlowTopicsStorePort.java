package org.example.testsproducer.application.port.out;

import java.util.Map;

public interface FlowTopicsStorePort {

    void save(Map<String, String> topics);
}
