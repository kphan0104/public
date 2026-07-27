package fr.itgp.testsproducer.application.port.out;

import fr.itgp.testsproducer.domain.model.DatabusEvent;

public interface EventSerializerPort {

    byte[] serialize(DatabusEvent event);
}
