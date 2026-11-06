package org.example.testsproducer.secretsanta;

import org.example.testsproducer.adapter.in.web.SecretSantaController;
import org.example.testsproducer.application.service.SecretSantaService;
import org.example.testsproducer.config.SecretSantaProperties;
import org.example.testsproducer.domain.model.SecretSantaDraw;
import org.example.testsproducer.domain.model.SecretSantaDraw.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecretSantaWebTest {
    private static final String ADMIN = "a".repeat(43);
    private static final String ALICE = "b".repeat(43);
    private static final String BOB = "c".repeat(43);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var service = new SecretSantaService(List.of("Alice", "<Bob & Co>"), create ->
                new SecretSantaDraw(List.of(new Entry("Alice", ALICE, "<Bob & Co>"),
                        new Entry("<Bob & Co>", BOB, "Alice"))));
        var properties = new SecretSantaProperties(true, List.of("Alice", "<Bob & Co>"),
                Path.of("unused"), "unused", ADMIN, URI.create("https://gifts.example.test/app/"));
        mvc = MockMvcBuilders.standaloneSetup(new SecretSantaController(service, properties)).build();
    }

    @Test
    void organizerGetsOnlyNamedLinksAndCopyButtonsWithConfiguredPublicBase() throws Exception {
        var response = mvc.perform(get("/secret-santa/admin/" + ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                .andReturn().getResponse();
        assertThat(response.getContentAsString()).contains("Alice", "&lt;Bob &amp; Co&gt;",
                "https://gifts.example.test/app/secret-santa/" + ALICE,
                "https://gifts.example.test/app/secret-santa/" + BOB, "Copier le lien")
                .doesNotContain("<Bob", "Tu offres un cadeau à", ADMIN, "class=\"recipient\"");
        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("frame-ancestors 'none'", "default-src 'none'", "script-src 'nonce-");
    }

    @Test
    void personalLinkOnlyExposesItsOwnResultAndEscapesHtml() throws Exception {
        var response = mvc.perform(get("/secret-santa/" + ALICE))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentAsString()).contains("Bonjour Alice", "&lt;Bob &amp; Co&gt;",
                "Tu offres un cadeau à").doesNotContain("<Bob", BOB, ADMIN);
    }

    @Test
    void invalidAdminAndParticipantSecretsReturnTheSameGeneric404() throws Exception {
        var admin = mvc.perform(get("/secret-santa/admin/wrong"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        var participant = mvc.perform(get("/secret-santa/wrong"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(admin).contains("Lien introuvable").doesNotContain("Alice", ALICE, ADMIN);
        assertThat(participant).contains("Lien introuvable").doesNotContain("Alice", ALICE, ADMIN);
    }
}
