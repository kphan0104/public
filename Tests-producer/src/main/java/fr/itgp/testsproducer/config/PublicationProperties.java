package fr.itgp.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("tests-producer.publication")
public record PublicationProperties(
        Duration acknowledgementTimeout,
        int maxMessageBytes
) {
    public PublicationProperties {
        if (acknowledgementTimeout == null
                || acknowledgementTimeout.isNegative()
                || acknowledgementTimeout.isZero()) {
            throw new IllegalArgumentException(
                    "acknowledgement-timeout doit être strictement positif"
            );
        }
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException(
                    "max-message-bytes doit être strictement positif"
            );
        }
    }
}
