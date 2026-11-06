package org.example.testsproducer.secretsanta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "tests-producer.publication.acknowledgement-timeout=10s",
        "tests-producer.publication.max-message-bytes=1000000",
        "tests-producer.flows.topics.payments=integration.events",
        "tests-producer.secret-santa.enabled=true",
        "tests-producer.secret-santa.participants[0]=Alice",
        "tests-producer.secret-santa.participants[1]=Bob",
        "tests-producer.secret-santa.public-base-url=https://gifts.example.test",
        "tests-producer.secret-santa.admin-key=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
})
@AutoConfigureMockMvc
class SecretSantaIntegrationTest {
    @TempDir static Path directory;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("tests-producer.secret-santa.storage-file", () -> directory.resolve("draw.enc").toString());
        registry.add("tests-producer.secret-santa.encryption-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void enablingCreatesStorageAndHtmlPagesWhileKeepingThemOutOfSwagger() throws Exception {
        assertThat(Files.isRegularFile(directory.resolve("draw.enc"))).isTrue();
        mvc.perform(get("/secret-santa/admin/" + "a".repeat(43))).andExpect(status().isOk());
        var response = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var paths = mapper.readTree(response.getContentAsByteArray()).get("paths");
        assertThat(paths.toString()).doesNotContain("secret-santa");
    }
}
