package org.example.testsproducer.adapter.out.json;

import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.FlowFormat;
import org.example.testsproducer.domain.model.Owner;
import org.example.testsproducer.domain.model.Provider;
import org.example.testsproducer.domain.model.Stage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DatabusEventJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JacksonEventSerializerAdapter serializer =
            new JacksonEventSerializerAdapter(objectMapper);

    @Test
    void serializesSeveralStagesDirectlyUnderLineage() throws Exception {
        var template = new DatabusEventTemplate(
                new Owner("itgp", "itgp", "itgp"),
                new Provider("itgp", "application"),
                new FlowFormat("1.0.0", "JSON"),
                "year",
                "MN",
                "integrations_tests",
                100
        );
        var event = template.create(
                "payments",
                "log",
                Instant.parse("2026-07-24T10:30:15Z"),
                "collect-host"
        ).withStage(
                2,
                new Stage(
                        Instant.parse("2026-07-24T10:30:16Z"),
                        "normalization_pipeline",
                        "transform-host",
                        0,
                        "PAR",
                        75
                )
        ).withEventSize(1234);

        var json = objectMapper.readTree(serializer.serialize(event));
        var lineage = json.get("databus").get("event").get("lineage");

        assertThat(lineage.get("last_stage").asInt()).isEqualTo(2);
        assertThat(lineage.get("stage1").get("pipeline_id").asText())
                .isEqualTo("integrations_tests");
        assertThat(lineage.get("stage1").get("event_size").asInt())
                .isZero();
        assertThat(lineage.get("stage2").get("pipeline_id").asText())
                .isEqualTo("normalization_pipeline");
        assertThat(lineage.get("stage2").get("event_size").asInt())
                .isEqualTo(1234);
        assertThat(lineage.get("stages")).isNull();
    }
}
