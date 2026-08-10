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
            summary = "Publier un originalMessage dans Kafka",
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
                    description = "Flux Databus. Le topic associé est "
                            + "déterminé par flow-topics.yml.",
                    required = true
            )
            @RequestParam
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @Parameter(description = "Groupe propriétaire")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_GROUP
            )
            @NotBlank
            @Size(max = 255)
            String ownerGroup,

            @Parameter(description = "Entité propriétaire")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_ENTITY
            )
            @NotBlank
            @Size(max = 255)
            String ownerEntity,

            @Parameter(description = "Nom du propriétaire")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_OWNER_NAME
            )
            @NotBlank
            @Size(max = 255)
            String ownerName,

            @Parameter(description = "Nom du fournisseur")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PROVIDER_NAME
            )
            @NotBlank
            @Size(max = 255)
            String providerName,

            @Parameter(description = "Source du fournisseur")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PROVIDER_SOURCE
            )
            @NotBlank
            @Size(max = 255)
            String providerSource,

            @Parameter(description = "Version du format")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_FORMAT_VERSION
            )
            @NotBlank
            @Size(max = 50)
            String formatVersion,

            @Parameter(description = "Type du format")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_FORMAT_TYPE
            )
            @NotBlank
            @Size(max = 50)
            String formatType,

            @Parameter(description = "Durée de rétention")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_RETENTION
            )
            @NotBlank
            @Size(max = 50)
            String retention,

            @Parameter(description = "Localisation du stage 1")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_LOCATION
            )
            @NotBlank
            @Size(max = 255)
            String location,

            @Parameter(description = "Identifiant du pipeline du stage 1")
            @RequestParam(
                    defaultValue = EventTemplateFactory.DEFAULT_PIPELINE_ID
            )
            @NotBlank
            @Size(max = 255)
            String pipelineId,

            @Parameter(description = "Durée de traitement du stage 1 en ms")
            @RequestParam(
                    defaultValue = EventTemplateFactory
                            .DEFAULT_PROCESSING_DURATION_MS
            )
            @Min(0)
            int processingDurationMs,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Contenu texte de l'originalMessage",
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
        String normalizedFlow = flow.trim();
        String topic = flowTopics.topicFor(normalizedFlow);
        if (topic == null) {
            throw new UnknownFlowException(normalizedFlow);
        }

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
