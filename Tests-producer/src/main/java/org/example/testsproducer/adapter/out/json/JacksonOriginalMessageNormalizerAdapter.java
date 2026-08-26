package org.example.testsproducer.adapter.out.json;

import org.example.testsproducer.application.port.out.OriginalMessageNormalizerPort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class JacksonOriginalMessageNormalizerAdapter
        implements OriginalMessageNormalizerPort {

    private final ObjectMapper objectMapper;

    public JacksonOriginalMessageNormalizerAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String normalize(String originalMessage) {
        String candidate = originalMessage.strip();
        if (!candidate.startsWith("{") || !candidate.endsWith("}")) {
            return originalMessage;
        }

        try {
            var json = objectMapper.readTree(candidate);
            if (!json.isObject()) {
                return originalMessage;
            }
            return objectMapper.writeValueAsString(json);
        } catch (Exception ignored) {
            return originalMessage;
        }
    }
}
