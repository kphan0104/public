package org.example.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;

@ConfigurationProperties("tests-producer.admin")
public final class AdminProperties {

    public static final String TOKEN_HEADER = "X-Admin-Token";

    private final byte[] token;
    private final Path flowTopicsFile;

    public AdminProperties(String token, Path flowTopicsFile) {
        if (token == null || token.length() < 32) {
            throw new IllegalArgumentException(
                    "tests-producer.admin.token doit contenir au moins "
                            + "32 caractères"
            );
        }
        if (flowTopicsFile == null) {
            throw new IllegalArgumentException(
                    "tests-producer.admin.flow-topics-file est obligatoire"
            );
        }
        this.token = token.getBytes(StandardCharsets.UTF_8);
        this.flowTopicsFile = flowTopicsFile.toAbsolutePath().normalize();
    }

    public Path flowTopicsFile() {
        return flowTopicsFile;
    }

    public boolean matches(String candidate) {
        if (candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
                token,
                candidate.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String toString() {
        return "AdminProperties[token=REDACTED, flowTopicsFile="
                + flowTopicsFile + "]";
    }
}
