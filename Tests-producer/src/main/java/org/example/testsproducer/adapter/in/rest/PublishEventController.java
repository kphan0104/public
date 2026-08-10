package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.config.FlowTopicsProperties;
import org.example.testsproducer.domain.model.DatabusEventTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class PublishEventController {

    private final PublishEventUseCase publishEventUseCase;
    private final FlowTopicsProperties flowTopics;

    public PublishEventController(
            PublishEventUseCase publishEventUseCase,
            FlowTopicsProperties flowTopics
    ) {
        this.publishEventUseCase = publishEventUseCase;
        this.flowTopics = flowTopics;
    }

    @Operation(
            summary = "Publier avec les valeurs Databus par défaut",
            description = "Le topic Kafka est déterminé automatiquement à "
                    + "partir du flux sélectionné."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message publié"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête ou flux invalide",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Message trop volumineux",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Kafka indisponible",
                    content = @Content
            )
    })
    @PostMapping(
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublishEventResponse> publish(
            @Parameter(
                    description = "databus.flow.name",
                    required = true
            )
            @RequestParam
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "originalMessage",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    value = "2026-08-10 INFO Message de test"
                            )
                    )
            )
            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        return publishEvent(
                flow,
                originalMessage,
                EventTemplateFactory.defaults()
        );
    }

    @Operation(
            summary = "Publier avec des valeurs Databus personnalisées",
            description = "Le topic Kafka est déterminé automatiquement à "
                    + "partir du flux sélectionné."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message publié"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête ou flux invalide",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Message trop volumineux",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Kafka indisponible",
                    content = @Content
            )
    })
    @PostMapping(
            path = "/custom",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublishEventResponse> publishCustom(
            @Parameter(
                    description = "databus.flow.name",
                    required = true
            )
            @RequestParam
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @Parameter(description = "databus.flow.owner.group")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_GROUP
            )
            @NotBlank
            @Size(max = 255)
            String ownerGroup,

            @Parameter(description = "databus.flow.owner.entity")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_ENTITY
            )
            @NotBlank
            @Size(max = 255)
            String ownerEntity,

            @Parameter(description = "databus.flow.owner.name")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_NAME
            )
            @NotBlank
            @Size(max = 255)
            String ownerName,

            @Parameter(description = "databus.flow.provider.name")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PROVIDER_NAME
            )
            @NotBlank
            @Size(max = 255)
            String providerName,

            @Parameter(description = "databus.flow.provider.source")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PROVIDER_SOURCE
            )
            @NotBlank
            @Size(max = 255)
            String providerSource,

            @Parameter(description = "databus.flow.format.version")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_FORMAT_VERSION
            )
            @NotBlank
            @Size(max = 50)
            String formatVersion,

            @Parameter(description = "databus.flow.format.type")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_FORMAT_TYPE
            )
            @NotBlank
            @Size(max = 50)
            String formatType,

            @Parameter(description = "databus.flow.retention")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_RETENTION
            )
            @NotBlank
            @Size(max = 50)
            String retention,

            @Parameter(
                    description = "databus.event.lineage.stage1.location"
            )
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_LOCATION
            )
            @NotBlank
            @Size(max = 255)
            String location,

            @Parameter(
                    description = "databus.event.lineage.stage1.pipeline_id"
            )
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PIPELINE_ID
            )
            @NotBlank
            @Size(max = 255)
            String pipelineId,

            @Parameter(
                    description = "databus.event.lineage.stage1."
                            + "processing_duration_ms"
            )
            @RequestParam(
                    defaultValue = EventTemplateFactory
                            .DEFAULT_PROCESSING_DURATION_MS
            )
            @Min(0)
            int processingDurationMs,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "originalMessage",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    value = "2026-08-10 INFO Message de test"
                            )
                    )
            )
            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        DatabusEventTemplate eventTemplate = EventTemplateFactory.create(
                ownerGroup,
                ownerEntity,
                ownerName,
                providerName,
                providerSource,
                formatVersion,
                formatType,
                retention,
                location,
                pipelineId,
                processingDurationMs
        );
        return publishEvent(flow, originalMessage, eventTemplate);
    }

    private ResponseEntity<PublishEventResponse> publishEvent(
            String flow,
            String originalMessage,
            DatabusEventTemplate eventTemplate
    ) {
        String normalizedFlow = flow.trim();
        String topic = flowTopics.topicFor(normalizedFlow);
        if (topic == null) {
            throw new UnknownFlowException(normalizedFlow);
        }

        var command = new PublishEventCommand(
                topic,
                normalizedFlow,
                originalMessage,
                eventTemplate
        );
        var result = publishEventUseCase.publish(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PublishEventResponse.from(result));
    }
}
