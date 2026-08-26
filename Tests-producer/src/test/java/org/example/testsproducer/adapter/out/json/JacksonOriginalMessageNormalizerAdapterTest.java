package org.example.testsproducer.adapter.out.json;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonOriginalMessageNormalizerAdapterTest {

    private final JacksonOriginalMessageNormalizerAdapter normalizer =
            new JacksonOriginalMessageNormalizerAdapter(new ObjectMapper());

    @Test
    void compactsAJsonObjectAndRemovesItsFinalLineBreak() {
        String originalMessage = """
                {
                  "message": "paiement accepté",
                  "details": {
                    "amount": 42,
                    "text": "espace conservé"
                  }
                }
                """;

        assertThat(normalizer.normalize(originalMessage)).isEqualTo(
                "{\"message\":\"paiement accepté\",\"details\":"
                        + "{\"amount\":42,\"text\":\"espace conservé\"}}"
        );
    }

    @Test
    void preservesANonJsonMessageExactly() {
        String originalMessage = "2026-08-26 INFO paiement accepté\n";

        assertThat(normalizer.normalize(originalMessage))
                .isSameAs(originalMessage);
    }

    @Test
    void preservesAnInvalidJsonObjectExactly() {
        String originalMessage = "{message invalide}\n";

        assertThat(normalizer.normalize(originalMessage))
                .isSameAs(originalMessage);
    }

    @Test
    void preservesAJsonArrayExactly() {
        String originalMessage = "[\n  {\"message\": \"test\"}\n]\n";

        assertThat(normalizer.normalize(originalMessage))
                .isSameAs(originalMessage);
    }
}
