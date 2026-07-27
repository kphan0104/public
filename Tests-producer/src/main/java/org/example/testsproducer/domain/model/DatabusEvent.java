package org.example.testsproducer.domain.model;

import java.util.Objects;

public record DatabusEvent(
        Databus databus,
        Object originalMessage
) {
    public DatabusEvent {
        Objects.requireNonNull(databus, "databus");
        Objects.requireNonNull(originalMessage, "originalMessage");
    }

    public DatabusEvent withStage(int stageNumber, Stage stage) {
        Lineage lineage = databus.event().lineage().withStage(
                stageNumber,
                stage
        );
        return withLineage(lineage);
    }

    public DatabusEvent withEventSize(int eventSize) {
        Lineage lineage = databus.event()
                .lineage()
                .withLastStageEventSize(eventSize);
        return withLineage(lineage);
    }

    public int eventSize() {
        return databus.event().lineage().lastStageValue().eventSize();
    }

    public String timestamp() {
        return databus.event()
                .lineage()
                .lastStageValue()
                .timestamp()
                .toString();
    }

    private DatabusEvent withLineage(Lineage lineage) {
        EventMetadata event = new EventMetadata(lineage);
        return new DatabusEvent(
                new Databus(databus.flow(), event),
                originalMessage
        );
    }
}
