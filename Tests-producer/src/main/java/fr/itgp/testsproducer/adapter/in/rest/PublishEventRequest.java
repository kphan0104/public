package fr.itgp.testsproducer.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublishEventRequest(
        @NotBlank
        @Size(max = 249)
        @Pattern(regexp = "^(?!\\.{1,2}$)[a-zA-Z0-9._-]+$")
        String topic,

        @NotBlank
        @Size(max = 255)
        String flowName,

        @NotNull
        Object originalMessage
) {
}
