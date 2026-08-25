package org.example.testsproducer.adapter.in.rest;

import org.example.testsproducer.application.port.in.FlowAdministrationUseCase;
import org.example.testsproducer.application.port.in.FlowUpsertResult;
import org.example.testsproducer.application.port.in.OriginalMessageUpsertResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

@WebMvcTest(AdminFlowController.class)
class AdminFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FlowAdministrationUseCase flowAdministration;

    @Test
    void createsOrUpdatesAFlow() throws Exception {
        when(flowAdministration.upsertFlow("orders", "orders.events"))
                .thenReturn(new FlowUpsertResult(
                        "orders",
                        "orders.events",
                        true
                ));

        mockMvc.perform(put("/internal/flows/orders")
                        .queryParam("topic", "orders.events"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("created"))
                .andExpect(jsonPath("$.flow").value("orders"))
                .andExpect(jsonPath("$.topic").value("orders.events"));
    }

    @Test
    void createsOrUpdatesAnOriginalMessage() throws Exception {
        when(flowAdministration.upsertOriginalMessage(
                "orders",
                "{{NOW}} message"
        )).thenReturn(new OriginalMessageUpsertResult("orders", false));

        mockMvc.perform(put("/internal/flows/orders/original-message")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{{NOW}} message"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"))
                .andExpect(jsonPath("$.flow").value("orders"));
    }
}
