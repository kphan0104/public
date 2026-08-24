package org.example.testsproducer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    private static final String DEFAULT_MESSAGES_EXTENSION =
            "x-tests-producer-default-original-messages";

    @Bean
    OpenAPI testsProducerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tests Producer")
                        .version("1.0.0"));
    }

    @Bean
    @SuppressWarnings("unchecked")
    OpenApiCustomizer testsProducerOpenApiCustomizer(
            FlowTopicsProperties flowTopics,
            DefaultOriginalMessages defaultOriginalMessages
    ) {
        return openApi -> {
            openApi.setServers(null);
            openApi.setTags(List.of(
                    new Tag()
                            .name("originalMessage")
                            .description("Publication d'un originalMessage "
                                    + "dans Kafka avec les métadonnées "
                                    + "Databus"),
                    new Tag()
                            .name("RAW Message")
                            .description("Publication directe dans Kafka")
            ));
            openApi.addExtension(
                    DEFAULT_MESSAGES_EXTENSION,
                    defaultOriginalMessages.loadFor(
                            flowTopics.topics().keySet()
                    )
            );

            var pathItem = openApi.getPaths().get("/events");
            if (pathItem == null || pathItem.getPost() == null) {
                return;
            }
            pathItem.getPost().getParameters().stream()
                    .filter(parameter -> "flow".equals(parameter.getName()))
                    .findFirst()
                    .ifPresent(parameter -> {
                        var source = parameter.getSchema();
                        var flowSchema = new StringSchema();
                        flowSchema.setMinLength(source.getMinLength());
                        flowSchema.setMaxLength(source.getMaxLength());
                        flowSchema.setPattern(source.getPattern());
                        flowSchema.setEnum(new ArrayList<>(
                                flowTopics.topics().keySet()
                        ));
                        parameter.setSchema(flowSchema);
                    });
        };
    }

    @Bean
    DefaultOriginalMessages defaultOriginalMessages(
            SwaggerProperties properties
    ) {
        return new DefaultOriginalMessages(properties);
    }

    @Bean
    SwaggerIndexTransformer testsProducerSwaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider
    ) {
        return new TestsProducerSwaggerIndexTransformer(
                swaggerUiConfigProperties,
                swaggerUiOAuthProperties,
                swaggerWelcomeCommon,
                objectMapperProvider
        );
    }
}
