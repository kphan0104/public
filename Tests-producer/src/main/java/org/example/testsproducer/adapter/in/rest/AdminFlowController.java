package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.FlowAdministrationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/flows")
public class AdminFlowController {

    private final FlowAdministrationUseCase flowAdministration;

    public AdminFlowController(
            FlowAdministrationUseCase flowAdministration
    ) {
        this.flowAdministration = flowAdministration;
    }

    @PutMapping(path = "/{flow}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminFlowResponse> upsertFlow(
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
        var result = flowAdministration.upsertFlow(flow, topic);
        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(
                new AdminFlowResponse(
                        result.created() ? "created" : "updated",
                        result.flow(),
                        result.topic()
                )
        );
    }

    @PutMapping(
            path = "/{flow}/original-message",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AdminOriginalMessageResponse> upsertOriginalMessage(
            @PathVariable
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        var result = flowAdministration.upsertOriginalMessage(
                flow,
                originalMessage
        );
        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(
                new AdminOriginalMessageResponse(
                        result.created() ? "created" : "updated",
                        result.flow()
                )
        );
    }
}
