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
        "tests-producer.flows.topics.payments=integration.events"
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
        var operation = objectMapper.readTree(response)
                .get("paths")
                .get("/api/v1/events")
                .get("post");
        var parameters = operation.get("parameters");

        var flow = findParameter(parameters, "flow");
        assertThat(flow.get("required").asBoolean()).isTrue();
        assertThat(flow.get("schema").get("enum").get(0).asText())
                .isEqualTo("payments");

        var ownerGroup = findParameter(parameters, "ownerGroup");
        assertThat(ownerGroup.get("schema").get("default").asText())
                .isEqualTo("itgp");

        assertThat(
                operation.get("requestBody")
                        .get("content")
                        .get("text/plain")
                        .get("schema")
                        .get("type")
                        .asText()
        ).isEqualTo("string");
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
