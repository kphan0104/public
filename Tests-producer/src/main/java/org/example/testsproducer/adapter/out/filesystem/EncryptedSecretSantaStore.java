package org.example.testsproducer.adapter.out.filesystem;

import org.example.testsproducer.application.port.out.SecretSantaStorePort;
import org.example.testsproducer.domain.model.SecretSantaDraw;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Supplier;

public final class EncryptedSecretSantaStore implements SecretSantaStorePort {
    private static final byte[] VERSION = {'S', 'S', '0', '1'};
    private final Path file;
    private final SecretKeySpec key;
    private final ObjectMapper mapper;

    public EncryptedSecretSantaStore(Path file, String encryptionKey, ObjectMapper mapper) {
        this.file = file.toAbsolutePath().normalize();
        this.mapper = mapper;
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptionKey == null ? "" : encryptionKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Secret Santa : encryption-key doit être une clé AES de 32 octets encodée en Base64.");
        }
        if (decoded.length != 32) {
            throw new IllegalArgumentException("Secret Santa : encryption-key doit être une clé AES de 32 octets encodée en Base64.");
        }
        key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public SecretSantaDraw loadOrCreate(Supplier<SecretSantaDraw> create) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            // The lock covers both the existence check and the atomic publication.
            try (FileChannel channel = FileChannel.open(file.resolveSibling(file.getFileName() + ".lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var lock = channel.lock()) {
                if (Files.exists(file)) {
                    return decrypt(Files.readAllBytes(file));
                }
                SecretSantaDraw draw = create.get();
                temporary = Files.createTempFile(file.getParent(), ".secret-santa-", ".tmp");
                if (temporary.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                    Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
                }
                byte[] content = encrypt(draw);
                try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.wrap(content);
                    while (buffer.hasRemaining()) output.write(buffer);
                    output.force(true);
                }
                // No non-atomic fallback: failure must not leave a partially written draw.
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
                return draw;
            }
        } catch (Exception exception) {
            // Do not include serialization errors or their causes: they may contain names/results.
            throw new IllegalStateException("Secret Santa : stockage inaccessible, fichier invalide ou clé incorrecte. Aucun nouveau tirage effectué.");
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (java.io.IOException ignored) { }
            }
        }
    }

    private byte[] encrypt(SecretSantaDraw draw) throws Exception {
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
        byte[] encrypted = cipher.doFinal(mapper.writeValueAsBytes(draw));
        return ByteBuffer.allocate(VERSION.length + nonce.length + encrypted.length)
                .put(VERSION).put(nonce).put(encrypted).array();
    }

    private SecretSantaDraw decrypt(byte[] content) throws Exception {
        if (content.length < 32 || !Arrays.equals(VERSION, Arrays.copyOf(content, 4))) {
            throw new IllegalArgumentException();
        }
        Cipher cipher = cipher(Cipher.DECRYPT_MODE, Arrays.copyOfRange(content, 4, 16));
        return mapper.readValue(cipher.doFinal(Arrays.copyOfRange(content, 16, content.length)), SecretSantaDraw.class);
    }

    private Cipher cipher(int mode, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(VERSION);
        return cipher;
    }
}
