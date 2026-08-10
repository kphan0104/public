package org.example.testsproducer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI testsProducerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("tests-producer API")
                .description(
                        "Publication d'originalMessages de tests dans Kafka"
                )
                .version("1.0.0"));
    }

    @Bean
    @SuppressWarnings("unchecked")
    OpenApiCustomizer flowSelectorCustomizer(
            FlowTopicsProperties flowTopics
    ) {
        return openApi -> {
            List.of("/api/v1/events", "/api/v1/events/custom")
                    .stream()
                    .map(openApi.getPaths()::get)
                    .filter(pathItem -> pathItem != null
                            && pathItem.getPost() != null)
                    .map(pathItem -> pathItem.getPost().getParameters())
                    .flatMap(List::stream)
                    .filter(parameter -> "flow".equals(parameter.getName()))
                    .forEach(parameter -> parameter.getSchema().setEnum(
                            new ArrayList<>(flowTopics.topics().keySet())
                    ));
        };
    }
}
