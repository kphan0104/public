package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishEventCommand;
import org.example.testsproducer.application.port.in.PublishEventResult;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

@WebMvcTest(PublishEventController.class)
class PublishEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishEventUseCase useCase;

    @Test
    void publishesAnEvent() throws Exception {
        when(useCase.publish(any())).thenReturn(
                new PublishEventResult(
                        "integration.events",
                        1,
                        9L,
                        640,
                        "2026-07-24T10:00:00Z"
                )
        );

        mockMvc.perform(multipart("/api/v1/events")
                .file(jsonFile("{\"id\":12}"))
                .param("topic", "integration.events")
                .param("flowName", "payments"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.topic").value("integration.events"))
                .andExpect(jsonPath("$.partition").value(1))
                .andExpect(jsonPath("$.offset").value(9))
                .andExpect(jsonPath("$.eventSize").value(640));

        var command = ArgumentCaptor.forClass(PublishEventCommand.class);
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().originalMessage())
                .isEqualTo(Map.of("id", 12));
    }

    @Test
    void rejectsARequestWithoutOriginalMessage() throws Exception {
        mockMvc.perform(multipart("/api/v1/events")
                .param("topic", "integration.events")
                .param("flowName", "payments"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.originalMessage").isNotEmpty()
                );
    }

    @Test
    void rejectsAnInvalidTopic() throws Exception {
        mockMvc.perform(multipart("/api/v1/events")
                .file(jsonFile("{\"message\":\"log\"}"))
                .param("topic", "topic interdit")
                .param("flowName", "payments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.topic").isNotEmpty());
    }

    @Test
    void rejectsAnInvalidJsonFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/events")
                .file(jsonFile("{json invalide}"))
                .param("topic", "integration.events")
                .param("flowName", "payments"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("originalMessage JSON invalide")
                );
    }

    private static MockMultipartFile jsonFile(String content) {
        return new MockMultipartFile(
                "originalMessage",
                "originalMessage.json",
                MediaType.APPLICATION_JSON_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
