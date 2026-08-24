package org.example.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("tests-producer.swagger")
public record SwaggerProperties(Path originalMessagesDirectory) {

    public SwaggerProperties {
        if (originalMessagesDirectory == null) {
            originalMessagesDirectory = Path.of("original-messages");
        }
    }
}
