package fr.itgp.testsproducer.adapter.out.json;

import fr.itgp.testsproducer.application.port.out.EventSerializerPort;
import fr.itgp.testsproducer.domain.model.DatabusEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JacksonEventSerializerAdapter implements EventSerializerPort {

    private final ObjectMapper objectMapper;

    public JacksonEventSerializerAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(DatabusEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event.fields());
        } catch (Exception exception) {
            throw new EventSerializationException(
                    "Impossible de sérialiser l'événement Databus",
                    exception
            );
        }
    }
}
