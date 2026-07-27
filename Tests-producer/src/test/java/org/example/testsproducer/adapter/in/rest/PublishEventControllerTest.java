package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.PublishEventResult;
import org.example.testsproducer.application.port.in.PublishEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .post;
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

        mockMvc.perform(
                post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "integration.events",
                                  "flowName": "payments",
                                  "originalMessage": {
                                    "id": 12
                                  }
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.topic").value("integration.events"))
                .andExpect(jsonPath("$.partition").value(1))
                .andExpect(jsonPath("$.offset").value(9))
                .andExpect(jsonPath("$.eventSize").value(640));
    }

    @Test
    void rejectsARequestWithoutOriginalMessage() throws Exception {
        mockMvc.perform(
                post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "integration.events",
                                  "flowName": "payments"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errors.originalMessage").isNotEmpty()
                );
    }

    @Test
    void rejectsAnInvalidTopic() throws Exception {
        mockMvc.perform(
                post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "topic interdit",
                                  "flowName": "payments",
                                  "originalMessage": "log"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.topic").isNotEmpty());
    }
}
