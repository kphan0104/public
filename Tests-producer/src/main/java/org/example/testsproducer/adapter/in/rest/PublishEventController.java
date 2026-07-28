package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/events")
public class PublishEventController {

    private final PublishEventUseCase publishEventUseCase;

    public PublishEventController(PublishEventUseCase publishEventUseCase) {
        this.publishEventUseCase = publishEventUseCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublishEventResponse> publish(
            @RequestParam(required = false)
            @NotBlank
            @Size(max = 249)
            @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
            String topic,

            @RequestParam(required = false)
            @NotBlank
            @Size(max = 255)
            String flowName,

            @RequestPart(required = false)
            @NotNull
            MultipartFile originalMessage
    ) {
        var command = new PublishEventCommand(
                topic.trim(),
                flowName.trim(),
                readOriginalMessage(originalMessage)
        );
        var result = publishEventUseCase.publish(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PublishEventResponse.from(result));
    }

    private String readOriginalMessage(MultipartFile originalMessage) {
        if (originalMessage.isEmpty()) {
            throw new InvalidOriginalMessageException(
                    "Le fichier originalMessage est vide"
            );
        }
        try {
            return new String(
                    originalMessage.getBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new InvalidOriginalMessageException(
                    "Impossible de lire le fichier originalMessage",
                    exception
            );
        }
    }
}
