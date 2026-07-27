package fr.itgp.testsproducer.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DatabusEvent {

    public static final String EVENT_SIZE_KEY =
            "databus.event.lineage.stage1.event_size";
    public static final String TIMESTAMP_KEY =
            "databus.event.lineage.stage1.timestamp";

    private final Map<String, Object> fields;

    private DatabusEvent(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static DatabusEvent create(
            String flowName,
            Object originalMessage,
            Instant timestamp,
            String hostname
    ) {
        Objects.requireNonNull(flowName, "flowName");
        Objects.requireNonNull(originalMessage, "originalMessage");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(hostname, "hostname");

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("databus.flow.name", flowName);
        fields.put("databus.flow.owner.group", "itgp");
        fields.put("databus.flow.owner.entity", "itgp");
        fields.put("databus.flow.owner.name", "itgp");
        fields.put("databus.flow.provider.name", "itgp");
        fields.put("databus.flow.provider.source", "application");
        fields.put("databus.flow.format.version", "1.0.0");
        fields.put("databus.flow.format.type", "JSON");
        fields.put("databus.flow.retention", "year");
        fields.put("databus.event.lineage.last_stage", 1);
        fields.put("databus.event.lineage.stage1.location", "MN");
        fields.put(
                "databus.event.lineage.stage1.pipeline_id",
                "integrations_tests"
        );
        fields.put(TIMESTAMP_KEY, timestamp.toString());
        fields.put(
                "databus.event.lineage.stage1.processing_duration_ms",
                100
        );
        fields.put("databus.event.lineage.stage1.host", hostname);
        fields.put(EVENT_SIZE_KEY, 0);
        fields.put("originalMessage", originalMessage);
        return new DatabusEvent(fields);
    }

    public DatabusEvent withEventSize(int eventSize) {
        Map<String, Object> resizedFields = new LinkedHashMap<>(fields);
        resizedFields.put(EVENT_SIZE_KEY, eventSize);
        return new DatabusEvent(resizedFields);
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public int eventSize() {
        return (Integer) fields.get(EVENT_SIZE_KEY);
    }

    public String timestamp() {
        return (String) fields.get(TIMESTAMP_KEY);
    }
}
