package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.FlowManagementUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Flux",
        description = "Gestion des flux et de leurs originalMessages"
)
@RestController
@RequestMapping("/flows")
public class FlowController {

    private final FlowManagementUseCase flowManagement;

    public FlowController(
            FlowManagementUseCase flowManagement
    ) {
        this.flowManagement = flowManagement;
    }

    @Operation(summary = "Créer un flux ou modifier son topic")
    @PutMapping(path = "/{flow}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FlowResponse> upsertFlow(
            @PathVariable
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @RequestParam
            @NotBlank
            @Size(max = 249)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String topic
    ) {
        var result = flowManagement.upsertFlow(flow, topic);
        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(
                new FlowResponse(
                        result.created() ? "created" : "updated",
                        result.flow(),
                        result.topic()
                )
        );
    }

    @Operation(summary = "Supprimer un flux")
    @DeleteMapping(path = "/{flow}")
    public ResponseEntity<Void> deleteFlow(
            @PathVariable
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow
    ) {
        flowManagement.deleteFlow(flow);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Créer ou remplacer un originalMessage")
    @PutMapping(
            path = "/{flow}/original-message",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OriginalMessageResponse> upsertOriginalMessage(
            @PathVariable
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        var result = flowManagement.upsertOriginalMessage(
                flow,
                originalMessage
        );
        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(
                new OriginalMessageResponse(
                        result.created() ? "created" : "updated",
                        result.flow()
                )
        );
    }
}
