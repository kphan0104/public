package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishRawMessageCommand;
import org.example.testsproducer.application.port.in.PublishRawMessageResult;
import org.example.testsproducer.application.port.in.PublishRawMessageUseCase;
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

@WebMvcTest(PublishRawMessageController.class)
class PublishRawMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishRawMessageUseCase useCase;

    @Test
    void publishesTheRawRequestBodyWithoutModification() throws Exception {
        byte[] rawMessage = new byte[]{0, 1, -1, 10, 13, 65};
        when(useCase.publish(any())).thenReturn(
                new PublishRawMessageResult(
                        "raw.events",
                        2,
                        12L,
                        rawMessage.length
                )
        );

        mockMvc.perform(post("/raw-events")
                .queryParam("topic", "raw.events")
                .contentType(MediaType.TEXT_PLAIN)
                .content(rawMessage))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.topic").value("raw.events"))
                .andExpect(jsonPath("$.messageSize")
                        .value(rawMessage.length));

        var command = ArgumentCaptor.forClass(
                PublishRawMessageCommand.class
        );
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().topic()).isEqualTo("raw.events");
        assertThat(command.getValue().rawMessage()).isEqualTo(rawMessage);
    }

    @Test
    void rejectsAnEmptyRawMessage() throws Exception {
        mockMvc.perform(post("/raw-events")
                .queryParam("topic", "raw.events")
                .contentType(MediaType.TEXT_PLAIN)
                .content(new byte[0]))
                .andExpect(status().isBadRequest());
    }
}
