package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.example.testsproducer.domain.model.FlowFormat;
import org.example.testsproducer.domain.model.Owner;
import org.example.testsproducer.domain.model.Provider;

final class EventTemplateFactory {

    static final String DEFAULT_OWNER_GROUP = "itgp";
    static final String DEFAULT_OWNER_ENTITY = "itgp";
    static final String DEFAULT_OWNER_NAME = "itgp";
    static final String DEFAULT_PROVIDER_NAME = "itgp";
    static final String DEFAULT_PROVIDER_SOURCE = "application";
    static final String DEFAULT_FORMAT_VERSION = "1.0.0";
    static final String DEFAULT_FORMAT_TYPE = "JSON";
    static final String DEFAULT_RETENTION = "year";
    static final String DEFAULT_LOCATION = "MN";
    static final String DEFAULT_PIPELINE_ID = "integrations_tests";
    static final String DEFAULT_PROCESSING_DURATION_MS = "100";

    private EventTemplateFactory() {
    }

    static DatabusEventTemplate defaults() {
        return create(
                DEFAULT_OWNER_GROUP,
                DEFAULT_OWNER_ENTITY,
                DEFAULT_OWNER_NAME,
                DEFAULT_PROVIDER_NAME,
                DEFAULT_PROVIDER_SOURCE,
                DEFAULT_FORMAT_VERSION,
                DEFAULT_FORMAT_TYPE,
                DEFAULT_RETENTION,
                DEFAULT_LOCATION,
                DEFAULT_PIPELINE_ID,
                Integer.parseInt(DEFAULT_PROCESSING_DURATION_MS)
        );
    }

    static DatabusEventTemplate create(
            String ownerGroup,
            String ownerEntity,
            String ownerName,
            String providerName,
            String providerSource,
            String formatVersion,
            String formatType,
            String retention,
            String location,
            String pipelineId,
            int processingDurationMs
    ) {
        return new DatabusEventTemplate(
                new Owner(
                        ownerGroup.trim(),
                        ownerEntity.trim(),
                        ownerName.trim()
                ),
                new Provider(
                        providerName.trim(),
                        providerSource.trim()
                ),
                new FlowFormat(formatVersion.trim(), formatType.trim()),
                retention.trim(),
                location.trim(),
                pipelineId.trim(),
                processingDurationMs
        );
    }
}
