package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.exception.BatchPublicationException;
import org.example.testsproducer.application.exception.EventPublicationException;
import org.example.testsproducer.application.port.in.FlowManagementUseCase;
import org.example.testsproducer.application.port.in.PublishBatchCommand;
import org.example.testsproducer.application.port.in.PublishBatchResult;
import org.example.testsproducer.application.port.in.PublishBatchUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublishBatchController.class)
@Import(BatchVariablesParser.class)
class PublishBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishBatchUseCase useCase;

    @MockitoBean
    private FlowManagementUseCase flowManagement;

    @Test
    void acceptsJsonVariablesAndUsesTheConfiguredTopic() throws Exception {
        when(flowManagement.topicFor("payments")).thenReturn("payments.events");
        when(useCase.publish(any())).thenReturn(new PublishBatchResult("payments.events", 1000, 1000));

        mockMvc.perform(post("/events/batch").queryParam("flow", "payments")
                        .queryParam("count", "1000")
                        .queryParam("variables", PublishBatchController.VARIABLES_EXAMPLE)
                        .contentType(MediaType.TEXT_PLAIN).content("${USER}/${AMOUNT}/${STATUS}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publishedCount").value(1000));

        var command = ArgumentCaptor.forClass(PublishBatchCommand.class);
        verify(useCase).publish(command.capture());
        assertThat(command.getValue().topic()).isEqualTo("payments.events");
        assertThat(command.getValue().variables()).containsKeys("USER", "AMOUNT", "STATUS");
        assertThat(command.getValue().eventTemplate().provider().name()).isEqualTo("itgp");
    }

    @Test
    void rejectsInvalidCountsAndJsonBeforePublication() throws Exception {
        when(flowManagement.topicFor("payments")).thenReturn("payments.events");
        for (String count : new String[]{"0", "1001", "-1"}) {
            mockMvc.perform(post("/events/batch").queryParam("flow", "payments")
                            .queryParam("count", count).contentType(MediaType.TEXT_PLAIN).content("message"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/events/batch").queryParam("flow", "payments")
                        .queryParam("variables", "invalid").contentType(MediaType.TEXT_PLAIN).content("message"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Variables invalides"));
        mockMvc.perform(post("/events/batch").queryParam("flow", "unknown")
                        .contentType(MediaType.TEXT_PLAIN).content("message"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/events/batch").queryParam("flow", "payments")
                        .contentType(MediaType.TEXT_PLAIN).content(" "))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @Test
    void exposesTheExactKafkaErrorAndPartialBatchProgress() throws Exception {
        String error = "Not authorized to access topics: [payments.events]";
        when(flowManagement.topicFor("payments")).thenReturn("payments.events");
        when(useCase.publish(any())).thenThrow(new BatchPublicationException(
                new EventPublicationException(error, new RuntimeException(error)), 100, 42));

        mockMvc.perform(post("/events/batch").queryParam("flow", "payments")
                        .contentType(MediaType.TEXT_PLAIN).content("message"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(error))
                .andExpect(jsonPath("$.requestedCount").value(100))
                .andExpect(jsonPath("$.publishedCount").value(42))
                .andExpect(jsonPath("$.failedMessageIndex").value(43));
    }
}
