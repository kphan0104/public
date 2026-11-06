package org.example.testsproducer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties("tests-producer.secret-santa")
public record SecretSantaProperties(
        boolean enabled, List<String> participants, Path storageFile,
        String encryptionKey, String adminKey, URI publicBaseUrl
) {
    public SecretSantaProperties {
        if (enabled) {
            if (storageFile == null) {
                throw new IllegalArgumentException("Secret Santa : storage-file est obligatoire.");
            }
            if (adminKey == null || !adminKey.matches("[A-Za-z0-9_-]{32,128}")) {
                throw new IllegalArgumentException("Secret Santa : admin-key doit contenir 32 à 128 caractères URL-safe aléatoires.");
            }
            if (publicBaseUrl == null || !List.of("http", "https").contains(publicBaseUrl.getScheme())
                    || publicBaseUrl.getHost() == null || publicBaseUrl.getUserInfo() != null
                    || publicBaseUrl.getQuery() != null || publicBaseUrl.getFragment() != null) {
                throw new IllegalArgumentException("Secret Santa : public-base-url doit être une URL HTTP(S) absolue sans identifiants, query ni fragment.");
            }
        }
    }

    @Override
    public String toString() {
        return "SecretSantaProperties[secrets masqués]";
    }
}
