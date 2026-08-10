package org.example.testsproducer.adapter.in.rest;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/v1/internal/events")
public class ScriptPublishEventController {

    private final PublishEventUseCase publishEventUseCase;

    public ScriptPublishEventController(
            PublishEventUseCase publishEventUseCase
    ) {
        this.publishEventUseCase = publishEventUseCase;
    }

    @PostMapping(
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PublishEventResponse> publish(
            @RequestParam
            @NotBlank
            @Size(max = 255)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String flow,

            @RequestParam
            @NotBlank
            @Size(max = 249)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String topic,

            @RequestBody
            @NotBlank
            String originalMessage
    ) {
        var command = new PublishEventCommand(
                topic.trim(),
                flow.trim(),
                originalMessage,
                EventTemplateFactory.defaults()
        );
        var result = publishEventUseCase.publish(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PublishEventResponse.from(result));
    }
}
