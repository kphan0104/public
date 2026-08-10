package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventResult;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.example.testsproducer.config.FlowTopicsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

@WebMvcTest({
        PublishEventController.class,
        ScriptPublishEventController.class
})
class PublishEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishEventUseCase useCase;

    @MockitoBean
    private FlowTopicsProperties flowTopics;

    @Test
    void publishesAnEventWithConfiguredDefaults() throws Exception {
        when(flowTopics.topicFor("payments"))
                .thenReturn("integration.events");
        when(useCase.publish(any())).thenReturn(
                new PublishEventResult(
                        "integration.events",
                        1,
                        9L,
                        640,
                        "2026-07-24T10:00:00Z"
                )
        );

        mockMvc.perform(post("/api/v1/events")
                .queryParam("flow", "payments")
                .contentType(MediaType.TEXT_PLAIN)
                .content("2026-08-10 INFO paiement accepté"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.topic").value("integration.events"))
                .andExpect(jsonPath("$.partition").value(1))
                .andExpect(jsonPath("$.offset").value(9))
                .andExpect(jsonPath("$.eventSize").value(640));

        var command = ArgumentCaptor.forClass(PublishEventCommand.class);
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().topic())
                .isEqualTo("integration.events");
        assertThat(command.getValue().flowName()).isEqualTo("payments");
        assertThat(command.getValue().originalMessage())
                .isEqualTo("2026-08-10 INFO paiement accepté");
        assertThat(command.getValue().eventTemplate().owner().group())
                .isEqualTo("itgp");
        assertThat(command.getValue().eventTemplate().provider().source())
                .isEqualTo("application");
        assertThat(command.getValue().eventTemplate().format().version())
                .isEqualTo("1.0.0");
        assertThat(command.getValue().eventTemplate().retention())
                .isEqualTo("year");
        assertThat(command.getValue().eventTemplate().stage1Location())
                .isEqualTo("MN");
        assertThat(
                command.getValue()
                        .eventTemplate()
                        .stage1ProcessingDurationMs()
        ).isEqualTo(100);
    }

    @Test
    void acceptsSwaggerParameterOverrides() throws Exception {
        when(flowTopics.topicFor("payments"))
                .thenReturn("integration.events");
        when(useCase.publish(any())).thenReturn(
                new PublishEventResult(
                        "integration.events",
                        0,
                        1L,
                        100,
                        "2026-08-10T10:00:00Z"
                )
        );

        mockMvc.perform(post("/api/v1/events")
                .queryParam("flow", "payments")
                .queryParam("ownerGroup", "custom-group")
                .queryParam("formatType", "NDJSON")
                .queryParam("processingDurationMs", "250")
                .contentType(MediaType.TEXT_PLAIN)
                .content("message"))
                .andExpect(status().isCreated());

        var command = ArgumentCaptor.forClass(PublishEventCommand.class);
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().eventTemplate().owner().group())
                .isEqualTo("custom-group");
        assertThat(command.getValue().eventTemplate().format().type())
                .isEqualTo("NDJSON");
        assertThat(
                command.getValue()
                        .eventTemplate()
                        .stage1ProcessingDurationMs()
        ).isEqualTo(250);
    }

    @Test
    void rejectsARequestWithoutOriginalMessage() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .queryParam("flow", "payments")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUnknownFlow() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .queryParam("flow", "unknown")
                .contentType(MediaType.TEXT_PLAIN)
                .content("message log"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Flux inconnu"));
    }

    @Test
    void scriptEndpointUsesTheExplicitTopicAndDefaultMetadata()
            throws Exception {
        when(useCase.publish(any())).thenReturn(
                new PublishEventResult(
                        "new-flow.events",
                        0,
                        2L,
                        120,
                        "2026-08-10T10:00:00Z"
                )
        );

        mockMvc.perform(post("/api/v1/internal/events")
                .queryParam("flow", "new-flow")
                .queryParam("topic", "new-flow.events")
                .contentType(MediaType.TEXT_PLAIN)
                .content("message du nouveau flux"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("new-flow.events"));

        var command = ArgumentCaptor.forClass(PublishEventCommand.class);
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().flowName()).isEqualTo("new-flow");
        assertThat(command.getValue().topic()).isEqualTo("new-flow.events");
        assertThat(command.getValue().originalMessage())
                .isEqualTo("message du nouveau flux");
        assertThat(command.getValue().eventTemplate().owner().group())
                .isEqualTo("itgp");
        assertThat(command.getValue().eventTemplate().stage1PipelineId())
                .isEqualTo("integrations_tests");
    }
}
