package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.PublishRawMessageCommand;
import org.example.testsproducer.application.port.in.PublishRawMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/raw-events")
@Tag(
        name = "RAW Message",
        description = "Publication directe dans Kafka"
)
public class PublishRawMessageController {

    private final PublishRawMessageUseCase publishRawMessageUseCase;

    public PublishRawMessageController(
            PublishRawMessageUseCase publishRawMessageUseCase
    ) {
        this.publishRawMessageUseCase = publishRawMessageUseCase;
    }

    @Operation(
            summary = "Publier un message RAW dans Kafka"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message publié"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Message trop volumineux",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Erreur Kafka",
                    content = @Content(
                            mediaType = MediaType
                                    .APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    @PostMapping(
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublishRawMessageResponse> publish(
            @Parameter(description = "topic Kafka", required = true)
            @RequestParam
            @NotBlank
            @Size(max = 249)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String topic,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "rawMessage",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string")
                    )
            )
            @RequestBody
            @NotEmpty
            byte[] rawMessage
    ) {
        var command = new PublishRawMessageCommand(
                topic.trim(),
                rawMessage
        );
        var result = publishRawMessageUseCase.publish(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PublishRawMessageResponse.from(result));
    }
}
