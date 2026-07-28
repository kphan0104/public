package org.example.testsproducer.domain.model;

import java.time.Instant;
import java.util.Objects;

public record DatabusEventTemplate(
        Owner owner,
        Provider provider,
        FlowFormat format,
        String retention,
        String stage1Location,
        String stage1PipelineId,
        int stage1ProcessingDurationMs
) {
    public DatabusEventTemplate {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(format, "format");
        retention = DomainValue.requireNonBlank(retention, "retention");
        stage1Location = DomainValue.requireNonBlank(
                stage1Location,
                "stage1Location"
        );
        stage1PipelineId = DomainValue.requireNonBlank(
                stage1PipelineId,
                "stage1PipelineId"
        );
        if (stage1ProcessingDurationMs < 0) {
            throw new IllegalArgumentException(
                    "stage1ProcessingDurationMs ne peut pas être négatif"
            );
        }
    }

    public DatabusEvent create(
            String flowName,
            String originalMessage,
            Instant timestamp,
            String hostname
    ) {
        Flow flow = new Flow(
                flowName,
                owner,
                provider,
                format,
                retention
        );
        Stage stage1 = new Stage(
                timestamp,
                stage1PipelineId,
                hostname,
                0,
                stage1Location,
                stage1ProcessingDurationMs
        );
        Lineage lineage = Lineage.startWith(stage1);
        return new DatabusEvent(
                new Databus(flow, new EventMetadata(lineage)),
                originalMessage
        );
    }
}
