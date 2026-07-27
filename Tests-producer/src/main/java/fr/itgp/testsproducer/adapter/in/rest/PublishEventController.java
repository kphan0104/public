package fr.itgp.testsproducer.adapter.in.rest;

import fr.itgp.testsproducer.application.port.in.PublishEventCommand;
import fr.itgp.testsproducer.application.port.in.PublishEventUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class PublishEventController {

    private final PublishEventUseCase publishEventUseCase;

    public PublishEventController(PublishEventUseCase publishEventUseCase) {
        this.publishEventUseCase = publishEventUseCase;
    }

    @PostMapping
    public ResponseEntity<PublishEventResponse> publish(
            @Valid @RequestBody PublishEventRequest request
    ) {
        var command = new PublishEventCommand(
                request.topic().trim(),
                request.flowName().trim(),
                request.originalMessage()
        );
        var result = publishEventUseCase.publish(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PublishEventResponse.from(result));
    }
}
