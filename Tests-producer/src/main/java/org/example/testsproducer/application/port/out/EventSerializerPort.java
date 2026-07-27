package org.example.testsproducer.application.port.out;

import org.example.testsproducer.domain.model.DatabusEvent;

public interface EventSerializerPort {

    byte[] serialize(DatabusEvent event);
}
