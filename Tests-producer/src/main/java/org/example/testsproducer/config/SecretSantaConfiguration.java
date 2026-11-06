package org.example.testsproducer.config;

import org.example.testsproducer.adapter.out.filesystem.EncryptedSecretSantaStore;
import org.example.testsproducer.application.service.SecretSantaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "tests-producer.secret-santa.enabled", havingValue = "true")
@EnableConfigurationProperties(SecretSantaProperties.class)
public class SecretSantaConfiguration {
    @Bean
    SecretSantaService secretSantaService(SecretSantaProperties properties, ObjectMapper mapper) {
        return new SecretSantaService(properties.participants(),
                new EncryptedSecretSantaStore(properties.storageFile(), properties.encryptionKey(), mapper));
    }
}
