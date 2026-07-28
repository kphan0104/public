package org.example.testsproducer.adapter.out.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.testsproducer.domain.model.DatabusEvent;
import org.example.testsproducer.domain.model.Flow;
import org.example.testsproducer.domain.model.Lineage;
import org.example.testsproducer.domain.model.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

record DatabusEventJson(
        DatabusJson databus,
        String originalMessage
) {
    static DatabusEventJson from(DatabusEvent source) {
        Flow flow = source.databus().flow();
        FlowJson flowJson = new FlowJson(
                flow.name(),
                new OwnerJson(
                        flow.owner().group(),
                        flow.owner().entity(),
                        flow.owner().name()
                ),
                new ProviderJson(
                        flow.provider().name(),
                        flow.provider().source()
                ),
                new FormatJson(
                        flow.format().version(),
                        flow.format().type()
                ),
                flow.retention()
        );
        LineageJson lineageJson = LineageJson.from(
                source.databus().event().lineage()
        );
        return new DatabusEventJson(
                new DatabusJson(
                        flowJson,
                        new EventJson(lineageJson)
                ),
                source.originalMessage()
        );
    }

    record DatabusJson(FlowJson flow, EventJson event) {
    }

    record FlowJson(
            String name,
            OwnerJson owner,
            ProviderJson provider,
            FormatJson format,
            String retention
    ) {
    }

    record OwnerJson(String group, String entity, String name) {
    }

    record ProviderJson(String name, String source) {
    }

    record FormatJson(String version, String type) {
    }

    record EventJson(LineageJson lineage) {
    }

    static final class LineageJson {

        private final int lastStage;
        private final Map<String, StageJson> stages;

        private LineageJson(
                int lastStage,
                Map<String, StageJson> stages
        ) {
            this.lastStage = lastStage;
            this.stages = stages;
        }

        static LineageJson from(Lineage lineage) {
            Map<String, StageJson> stages = new LinkedHashMap<>();
            lineage.stages().forEach((number, stage) ->
                    stages.put("stage" + number, StageJson.from(stage))
            );
            return new LineageJson(lineage.lastStage(), stages);
        }

        @JsonProperty("last_stage")
        public int lastStage() {
            return lastStage;
        }

        @JsonAnyGetter
        public Map<String, StageJson> stages() {
            return stages;
        }
    }

    record StageJson(
            String timestamp,
            @JsonProperty("pipeline_id") String pipelineId,
            String host,
            @JsonProperty("event_size") int eventSize,
            String location,
            @JsonProperty("processing_duration_ms")
            int processingDurationMs
    ) {
        static StageJson from(Stage stage) {
            return new StageJson(
                    stage.timestamp().toString(),
                    stage.pipelineId(),
                    stage.host(),
                    stage.eventSize(),
                    stage.location(),
                    stage.processingDurationMs()
            );
        }
    }
}
