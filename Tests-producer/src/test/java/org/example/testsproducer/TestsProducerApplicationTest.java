package org.example.testsproducer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.producer.key-serializer="
                + "org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer="
                + "org.apache.kafka.common.serialization.ByteArraySerializer",
        "tests-producer.publication.acknowledgement-timeout=10s",
        "tests-producer.publication.max-message-bytes=1000000",
        "tests-producer.flows.topics.payments=integration.events",
        "tests-producer.swagger.original-messages-directory="
                + "src/test/resources/original-messages",
        "tests-producer.admin.token="
                + "tests-producer-admin-token-for-tests-123456",
        "tests-producer.admin.flow-topics-file="
                + "target/test-flow-topics.yml",
        "springdoc.cache.disabled=true",
        "springdoc.swagger-ui.default-models-expand-depth=-1",
        "springdoc.swagger-ui.doc-expansion=list",
        "springdoc.swagger-ui.try-it-out-enabled=true"
})
@AutoConfigureMockMvc
class TestsProducerApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void exposesSwaggerWithFlowSelectorAndDefaults() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var openApi = objectMapper.readTree(response);
        assertThat(openApi.get("info").get("title").asText())
                .isEqualTo("Tests Producer");
        assertThat(openApi.get("info").get("version").asText())
                .isEqualTo("1.0.0");
        assertThat(openApi.get("info").get("description")).isNull();
        assertThat(openApi.get("servers")).isNull();
        assertThat(
                openApi.get(
                                "x-tests-producer-default-original-messages"
                        )
                        .get("payments")
                        .asText()
        ).isEqualTo("{\"timestamp\":\"{{NOW|yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                + "|UTC}}\",\"message\":\"Paiement accepté\"}\n");
        var tags = openApi.get("tags");
        assertThat(tags.get(0).get("name").asText())
                .isEqualTo("originalMessage");
        assertThat(tags.get(0).get("description").asText())
                .isEqualTo("Publication d'un originalMessage dans Kafka "
                        + "avec les métadonnées Databus");
        assertThat(tags.get(1).get("name").asText())
                .isEqualTo("RAW Message");
        assertThat(tags.get(1).get("description").asText())
                .isEqualTo("Publication directe dans Kafka");

        var paths = openApi.get("paths");
        var defaultOperation = paths
                .get("/events")
                .get("post");
        var customOperation = paths
                .get("/events/custom")
                .get("post");
        assertThat(paths.get("/api/v1/events")).isNull();
        assertThat(paths.get("/api/v1/events/custom")).isNull();
        assertThat(paths.get("/api/v1/raw-events")).isNull();
        assertThat(paths.get("/internal/events")).isNull();
        assertThat(paths.get("/internal/flows/{flow}")).isNull();
        assertThat(
                paths.get("/internal/flows/{flow}/original-message")
        ).isNull();
        assertThat(paths.get("/raw-events").get("post")).isNotNull();
        assertThat(defaultOperation.get("tags").get(0).asText())
                .isEqualTo("originalMessage");
        assertThat(defaultOperation.get("summary").asText())
                .isEqualTo("Publier avec les valeurs Databus par défaut");
        assertThat(customOperation.get("summary").asText())
                .isEqualTo(
                        "Publier avec des valeurs Databus personnalisées"
                );
        assertThat(defaultOperation.get("description")).isNull();
        assertThat(customOperation.get("description")).isNull();
        assertThat(
                paths.get("/raw-events")
                        .get("post")
                        .get("tags")
                        .get(0)
                        .asText()
        ).isEqualTo("RAW Message");
        assertThat(
                paths.get("/raw-events")
                        .get("post")
                        .get("summary")
                        .asText()
        ).isEqualTo("Publier un message RAW dans Kafka");
        assertThat(
                paths.get("/raw-events")
                        .get("post")
                        .get("description")
        ).isNull();
        assertThat(
                paths.get("/raw-events")
                        .get("post")
                        .get("requestBody")
                        .get("content")
                        .get("text/plain")
                        .get("schema")
                        .get("type")
                        .asText()
        ).isEqualTo("string");
        var defaultParameters = defaultOperation.get("parameters");
        var customParameters = customOperation.get("parameters");

        var defaultFlow = findParameter(defaultParameters, "flow");
        assertThat(defaultFlow.get("required").asBoolean()).isTrue();
        assertThat(defaultFlow.get("schema").get("enum").toString())
                .contains("payments");
        assertThat(defaultParameters.size()).isEqualTo(1);

        var customFlow = findParameter(customParameters, "flow");
        assertThat(customFlow.get("schema").get("enum")).isNull();
        var topic = findParameter(customParameters, "topic");
        assertThat(topic.get("required").asBoolean()).isTrue();
        var ownerGroup = findParameter(customParameters, "ownerGroup");
        assertThat(ownerGroup.get("schema").get("default").asText())
                .isEqualTo("itgp");

        assertThat(
                defaultOperation.get("requestBody")
                        .get("content")
                        .get("text/plain")
                        .get("schema")
                        .get("type")
                        .asText()
        ).isEqualTo("string");
    }

    @Test
    void hidesSchemasInSwaggerUi() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var swaggerConfig = objectMapper.readTree(response);

        assertThat(swaggerConfig.get("defaultModelsExpandDepth").asInt())
                .isEqualTo(-1);
        assertThat(swaggerConfig.get("docExpansion").asText())
                .isEqualTo("list");
        assertThat(swaggerConfig.get("tryItOutEnabled").asBoolean())
                .isTrue();
        assertThat(swaggerConfig.get("url").asText())
                .isEqualTo("/v3/api-docs");
    }

    @Test
    void customizesSwaggerUiWithoutOpeningOperations() throws Exception {
        String initializer = mockMvc.perform(
                        get("/swagger-ui/swagger-initializer.js")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(initializer)
                .contains(".swagger-ui .topbar")
                .contains(".swagger-ui .info .link")
                .contains(".opblock-section-request-body")
                .contains("x-tests-producer-default-original-messages")
                .contains("function resolveTimestamps(messageTemplate)")
                .contains("message = resolveTimestamps(messageTemplate)")
                .contains("\"docExpansion\" : \"list\"")
                .contains("\"tryItOutEnabled\" : true");
    }

    @Test
    void exposesASeparateAdminSwagger() throws Exception {
        String page = mockMvc.perform(get("/admin/swagger-ui.html"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(page)
                .contains("Tests Producer Administration")
                .contains("/admin/api-docs")
                .contains("SwaggerUIBundle");

        var response = mockMvc.perform(get("/admin/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var adminOpenApi = objectMapper.readTree(response);
        assertThat(
                adminOpenApi.get("paths").get("/internal/flows/{flow}")
        ).isNotNull();
        assertThat(
                adminOpenApi.get("paths")
                        .get("/internal/flows/{flow}/original-message")
        ).isNotNull();
        assertThat(
                adminOpenApi.get("components")
                        .get("securitySchemes")
                        .get("AdminToken")
                        .get("name")
                        .asText()
        ).isEqualTo("X-Admin-Token");
    }

    @Test
    void protectsInternalEndpointsWithTheAdminToken() throws Exception {
        mockMvc.perform(put("/internal/flows/payments")
                        .queryParam("topic", "integration.events"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/internal/flows/payments")
                        .header(
                                "X-Admin-Token",
                                "tests-producer-admin-token-for-tests-123456"
                        )
                        .queryParam("topic", "integration.events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"))
                .andExpect(jsonPath("$.flow").value("payments"))
                .andExpect(jsonPath("$.topic").value("integration.events"));
    }

    @Test
    void exposesANewFlowInSwaggerWithoutRestarting() throws Exception {
        mockMvc.perform(put("/internal/flows/dynamic-flow")
                        .header(
                                "X-Admin-Token",
                                "tests-producer-admin-token-for-tests-123456"
                        )
                        .queryParam("topic", "dynamic.events"))
                .andExpect(status().isCreated());

        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var openApi = objectMapper.readTree(response);
        var flow = findParameter(
                openApi.get("paths")
                        .get("/events")
                        .get("post")
                        .get("parameters"),
                "flow"
        );
        assertThat(flow.get("schema").get("enum").toString())
                .contains("dynamic-flow");
    }

    private static tools.jackson.databind.JsonNode findParameter(
            tools.jackson.databind.JsonNode parameters,
            String name
    ) {
        for (var parameter : parameters) {
            if (name.equals(parameter.get("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("Paramètre OpenAPI absent : " + name);
    }
}
