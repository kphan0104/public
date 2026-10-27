package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.FlowManagementUseCase;
import org.example.testsproducer.application.port.in.PublishBatchCommand;
import org.example.testsproducer.application.port.in.PublishBatchUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "originalMessage")
public class PublishBatchController {

    static final String VARIABLES_EXAMPLE = """
            {
              "USER": {"type": "string", "length": 8, "characters": "a-z"},
              "AMOUNT": {"type": "integer", "min": 1, "max": 10000},
              "STATUS": {"type": "choice", "values": ["OK", "ERROR", "PENDING"]}
            }
            """;

    private final PublishBatchUseCase publishBatch;
    private final FlowManagementUseCase flowManagement;
    private final BatchVariablesParser variablesParser;

    public PublishBatchController(
            PublishBatchUseCase publishBatch,
            FlowManagementUseCase flowManagement,
            BatchVariablesParser variablesParser
    ) {
        this.publishBatch = publishBatch;
        this.flowManagement = flowManagement;
        this.variablesParser = variablesParser;
    }

    @Operation(summary = "Publier un lot de messages avec des variables aléatoires")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lot publié"),
            @ApiResponse(responseCode = "400", description = "Requête, flux ou variables invalides", content = @Content),
            @ApiResponse(responseCode = "413", description = "Message trop volumineux", content = @Content),
            @ApiResponse(responseCode = "503", description = "Erreur Kafka",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(
            path = "/events/batch",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublishBatchResponse> publish(
            @Parameter(description = "databus.flow.name", required = true)
            @RequestParam(name = "flow")
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @Parameter(description = "Nombre de messages à publier, de 1 à 1000")
            @RequestParam(defaultValue = "100")
            @Min(1)
            @Max(1000)
            int count,

            @Parameter(
                    description = "Règles JSON des variables : string (length, characters), "
                            + "integer (min, max inclus), choice (values). {} pour répéter le message sans variable.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(value = VARIABLES_EXAMPLE)
                    )
            )
            @RequestParam(required = false)
            String variables,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "originalMessage",
                    required = true,
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = "Utilisateur=${USER}, montant=${AMOUNT}, statut=${STATUS}"))
            )
            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        String normalizedFlow = flow.trim();
        String topic = flowManagement.topicFor(normalizedFlow);
        if (topic == null) {
            throw new UnknownFlowException(normalizedFlow);
        }
        var command = new PublishBatchCommand(
                topic,
                normalizedFlow,
                originalMessage,
                count,
                variablesParser.parse(variables),
                EventTemplateFactory.defaults()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PublishBatchResponse.from(publishBatch.publish(command)));
    }
}
