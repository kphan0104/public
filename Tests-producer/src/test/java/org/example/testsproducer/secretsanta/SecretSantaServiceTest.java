package org.example.testsproducer.secretsanta;

import org.example.testsproducer.adapter.out.filesystem.EncryptedSecretSantaStore;
import org.example.testsproducer.application.service.SecretSantaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class SecretSantaServiceTest {
    @TempDir Path directory;
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final List<String> NAMES = List.of("Alice", "Bob", "Charlie", "Diane");

    @Test
    void everybodyGivesAndReceivesOnceWithoutDrawingThemselves() {
        for (int size = 2; size <= 20; size++) {
            var names = IntStream.range(0, size).mapToObj(i -> "Personne " + i).toList();
            for (int attempt = 0; attempt < 20; attempt++) {
                var service = new SecretSantaService(names, java.util.function.Supplier::get);
                assertThat(service.links()).hasSize(size);
                assertThat(service.links().stream().map(link -> link.token()).distinct().count()).isEqualTo(size);
                var recipients = new ArrayList<String>();
                for (var link : service.links()) {
                    var reveal = service.reveal(link.token()).orElseThrow();
                    assertThat(reveal.participant()).isEqualTo(link.participant());
                    assertThat(reveal.recipient()).isNotEqualTo(reveal.participant());
                    recipients.add(reveal.recipient());
                }
                assertThat(recipients).containsExactlyInAnyOrderElementsOf(names);
            }
        }
    }

    @Test
    void encryptedDrawAndEveryLinkSurviveRestartAndParticipantReordering() throws Exception {
        var first = service(NAMES, KEY);
        byte[] saved = Files.readAllBytes(file());
        String raw = new String(saved, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String name : NAMES) assertThat(raw).doesNotContain(name);
        for (var link : first.links()) assertThat(raw).doesNotContain(link.token());
        var reordered = new ArrayList<>(NAMES);
        Collections.reverse(reordered);
        var restarted = service(reordered, KEY);
        assertThat(restarted.links()).isEqualTo(first.links());
        for (var link : first.links()) {
            assertThat(restarted.reveal(link.token())).isEqualTo(first.reveal(link.token()));
        }
        assertThat(Files.readAllBytes(file())).isEqualTo(saved);
        if (file().getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertThat(Files.getPosixFilePermissions(file()))
                    .isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        }
    }

    @Test
    void wrongKeyAndChangedParticipantsNeverReplaceTheDraw() throws Exception {
        service(NAMES, KEY);
        byte[] saved = Files.readAllBytes(file());
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        assertThatThrownBy(() -> service(NAMES, Base64.getEncoder().encodeToString(otherKey)))
                .isInstanceOf(IllegalStateException.class).hasNoCause();
        assertThatThrownBy(() -> service(List.of("Alice", "Bob"), KEY))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(file())).isEqualTo(saved);
    }

    @Test
    void corruptedOrEmptyStorageNeverTriggersANewDraw() throws Exception {
        service(NAMES, KEY);
        byte[] bytes = Files.readAllBytes(file());
        bytes[bytes.length - 1] ^= 1;
        Files.write(file(), bytes);
        assertThatThrownBy(() -> service(NAMES, KEY)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(file())).isEqualTo(bytes);
        Files.write(file(), new byte[0]);
        assertThatThrownBy(() -> service(NAMES, KEY)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.size(file())).isZero();
    }

    @Test
    void invalidNamesDoNotCreateAFileAndUnknownTokensRevealNothing() {
        for (var names : List.of(List.<String>of(), List.of("Alice"), List.of("Alice", "Alice"),
                List.of("Alice", " "), List.of("Alice", " Bob"))) {
            assertThatThrownBy(() -> service(names, KEY)).isInstanceOf(IllegalArgumentException.class);
            assertThat(file()).doesNotExist();
        }
        assertThat(service(NAMES, KEY).reveal("unknown")).isEmpty();
    }

    private Path file() { return directory.resolve("draw.enc"); }

    private SecretSantaService service(List<String> names, String key) {
        return new SecretSantaService(names, new EncryptedSecretSantaStore(file(), key, JsonMapper.builder().build()));
    }
}
