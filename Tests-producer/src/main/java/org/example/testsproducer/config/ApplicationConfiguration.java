package org.example.testsproducer.config;

import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.application.port.out.EventSerializerPort;
import org.example.testsproducer.application.port.out.HostnameProviderPort;
import org.example.testsproducer.application.service.PublishEventService;
import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.FlowFormat;
import org.example.testsproducer.domain.model.Owner;
import org.example.testsproducer.domain.model.Provider;
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
    DatabusEventTemplate databusEventTemplate() {
        Owner owner = new Owner("itgp", "itgp", "itgp");
        Provider provider = new Provider("itgp", "application");
        FlowFormat format = new FlowFormat("1.0.0", "JSON");
        return new DatabusEventTemplate(
                owner,
                provider,
                format,
                "year",
                "MN",
                "integrations_tests",
                100
        );
    }

    @Bean
    PublishEventUseCase publishEventUseCase(
            EventPublisherPort eventPublisher,
            EventSerializerPort eventSerializer,
            HostnameProviderPort hostnameProvider,
            Clock clock,
            PublicationProperties publicationProperties,
            DatabusEventTemplate databusEventTemplate
    ) {
        return new PublishEventService(
                eventPublisher,
                eventSerializer,
                hostnameProvider,
                clock,
                databusEventTemplate,
                publicationProperties.maxMessageBytes()
        );
    }
}
