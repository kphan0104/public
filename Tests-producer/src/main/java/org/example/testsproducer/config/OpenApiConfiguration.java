package org.example.testsproducer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

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
            var pathItem = openApi.getPaths().get("/api/v1/events");
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
}
