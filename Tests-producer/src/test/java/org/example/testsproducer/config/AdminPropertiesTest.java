package org.example.testsproducer.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AdminPropertiesTest {

    @Test
    void acceptsATokenContainingFiveCharacters() {
        AdminProperties properties = new AdminProperties(
                "abcde",
                Path.of("flow-topics.yml")
        );

        assertThat(properties.matches("abcde")).isTrue();
    }

    @Test
    void rejectsATokenContainingFewerThanFiveCharacters() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AdminProperties(
                        "abcd",
                        Path.of("flow-topics.yml")
                ))
                .withMessageContaining("au moins 5 caractères");
    }
}
