package fr.itgp.testsproducer.config;

import fr.itgp.testsproducer.application.port.in.PublishEventUseCase;
import fr.itgp.testsproducer.application.port.out.EventPublisherPort;
import fr.itgp.testsproducer.application.port.out.EventSerializerPort;
import fr.itgp.testsproducer.application.port.out.HostnameProviderPort;
import fr.itgp.testsproducer.application.service.PublishEventService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PublicationProperties.class)
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
            PublicationProperties properties
    ) {
        return new PublishEventService(
                eventPublisher,
                eventSerializer,
                hostnameProvider,
                clock,
                properties.maxMessageBytes()
        );
    }
}
