package org.example.testsproducer.config;

import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.application.port.in.PublishRawMessageUseCase;
import org.example.testsproducer.application.port.in.FlowAdministrationUseCase;
import org.example.testsproducer.application.port.out.EventPublisherPort;
import org.example.testsproducer.application.port.out.EventSerializerPort;
import org.example.testsproducer.application.port.out.FlowTopicsStorePort;
import org.example.testsproducer.application.port.out.HostnameProviderPort;
import org.example.testsproducer.application.port.out.OriginalMessageStorePort;
import org.example.testsproducer.application.service.FlowAdministrationService;
import org.example.testsproducer.application.service.PublishEventService;
import org.example.testsproducer.application.service.PublishRawMessageService;
import org.example.testsproducer.adapter.out.filesystem.FileSystemFlowTopicsStoreAdapter;
import org.example.testsproducer.adapter.out.filesystem.FileSystemOriginalMessageStoreAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        PublicationProperties.class,
        FlowTopicsProperties.class,
        SwaggerProperties.class,
        AdminProperties.class
})
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FlowTopicsStorePort flowTopicsStore(AdminProperties properties) {
        return new FileSystemFlowTopicsStoreAdapter(properties);
    }

    @Bean
    OriginalMessageStorePort originalMessageStore(
            SwaggerProperties properties
    ) {
        return new FileSystemOriginalMessageStoreAdapter(properties);
    }

    @Bean
    FlowAdministrationUseCase flowAdministrationUseCase(
            FlowTopicsProperties flowTopicsProperties,
            FlowTopicsStorePort flowTopicsStore,
            OriginalMessageStorePort originalMessageStore,
            PublicationProperties publicationProperties
    ) {
        return new FlowAdministrationService(
                flowTopicsProperties.topics(),
                flowTopicsStore,
                originalMessageStore,
                publicationProperties.maxMessageBytes()
        );
    }

    @Bean
    FilterRegistrationBean<AdminTokenFilter> adminTokenFilter(
            AdminProperties properties
    ) {
        FilterRegistrationBean<AdminTokenFilter> registration =
                new FilterRegistrationBean<>();
        registration.setFilter(new AdminTokenFilter(properties));
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
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

    @Bean
    PublishRawMessageUseCase publishRawMessageUseCase(
            EventPublisherPort eventPublisher,
            PublicationProperties publicationProperties
    ) {
        return new PublishRawMessageService(
                eventPublisher,
                publicationProperties.maxMessageBytes()
        );
    }
}
