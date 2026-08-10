package org.example.testsproducer.config;

import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.application.port.out.EventSerializerPort;
import org.example.testsproducer.application.port.out.HostnameProviderPort;
import org.example.testsproducer.application.service.PublishEventService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        PublicationProperties.class,
        FlowTopicsProperties.class
})
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PublishEventUseCase publishEventUseCase(
            EventPublisherPort eventPublisher,
            EventSerializerPort eventSerializer,
            HostnameProviderPort hostnameProvider,
            Clock clock,
            PublicationProperties publicationProperties
    ) {
        return new PublishEventService(
                eventPublisher,
                eventSerializer,
                hostnameProvider,
                clock,
                publicationProperties.maxMessageBytes()
        );
    }
}
