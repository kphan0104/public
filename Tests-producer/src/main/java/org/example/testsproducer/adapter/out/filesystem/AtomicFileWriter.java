package org.example.testsproducer.adapter.out.filesystem;

import org.example.testsproducer.application.exception.AdministrationStorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicFileWriter {

    private AtomicFileWriter() {
    }

    static void write(Path target, String content) {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(
                    parent,
                    ".tests-producer-",
                    ".tmp"
            );
            Files.writeString(
                    temporaryFile,
                    content,
                    StandardCharsets.UTF_8
            );
            move(temporaryFile, absoluteTarget);
        } catch (IOException exception) {
            throw new AdministrationStorageException(
                    "Impossible d'écrire le fichier " + absoluteTarget,
                    exception
            );
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Le fichier temporaire sera nettoyé par l'exploitation.
                }
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}
